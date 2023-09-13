package com.yueerba.framework.cache.manage.support;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.yueerba.framework.cache.manage.consumer.CacheDelayedConsumer;
import com.yueerba.framework.cache.manage.producer.CacheDelayedProducer;
import com.yueerba.framework.cache.manage.sync.DoubleCheckLocking;
import com.yueerba.framework.cache.manage.sync.RedisDistributedLock;
import com.yueerba.framework.cache.properties.CacheConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.yueerba.framework.cache.properties.CacheConfigProperties.CACHE_PREFIX;
import static com.yueerba.framework.cache.properties.CacheConfigProperties.KEY_SEGMENTATION;

/**
 * Description:
 * <p>
 * 这是一个双层缓存实现，第一层是基于Caffeine的本地缓存，第二层是基于Redis的分布式缓存。
 * 它利用双重检查锁和Redis分布式锁来确保高并发环境下的线程安全。
 * <p>
 * Author: yueerba
 * Date: 2023/9/12
 */
@Slf4j
public class RedisCaffeineCache extends AbstractValueAdaptingCache implements MultiCache {

    /**
     * 缓存名称
     */
    private final String cacheName;

    /**
     * 缓存名前缀，用于生成缓存key
     */
    private final String cacheNamePrefix;

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redisson客户端
     */
    private final RedissonClient redissonClient;

    /**
     * Caffeine本地缓存
     */
    private final Cache<String, Object> caffeineCache;

    /**
     * 缓存配置属性
     */
    private final CacheConfigProperties cacheConfigProperties;

    /**
     * Redis分布式锁工具
     */
    private final RedisDistributedLock redisDistributedLock;

    /**
     * 双重检查锁策略
     */
    private final DoubleCheckLocking doubleCheckLocking;

    /**
     * 用于缓存键的布隆过滤器
     */
    private final RBloomFilter<String> bloomFilter;

    /**
     * 缓存延迟同步消费者
     */
    private final CacheDelayedConsumer cacheDelayedConsumer;

    /**
     * 缓存延迟同步生产者
     */
    private final CacheDelayedProducer cacheDelayedProducer;

    /**
     * 特殊的空值用于解决缓存穿透问题
     */
    private static final String NULL_VALUE = "CUSTOM_NULL_VALUE";

    /**
     * 空值在Redis中的过期时间（秒）
     */
    private static final int NULL_VALUE_EXPIRE_TIME = 300;

    /**
     * 构造函数，用于初始化必要的依赖项。
     *
     * @param cacheName 缓存名
     * @param cacheConfigProperties 缓存配置属性
     * @param caffeineCache Caffeine本地缓存
     * @param redisTemplate Redis操作模板
     * @param redissonClient Redisson客户端
     * @param redisDistributedLock Redis分布式锁工具
     * @param doubleCheckLocking 双重检查锁策略
     * @param cacheDelayedConsumer 缓存延迟同步消费者
     * @param cacheDelayedProducer 缓存延迟同步生产者
     */
    public RedisCaffeineCache(String cacheName,
                              CacheConfigProperties cacheConfigProperties,
                              Cache<String, Object> caffeineCache,
                              RedisTemplate<String, Object> redisTemplate,
                              RedissonClient redissonClient,
                              RedisDistributedLock redisDistributedLock,
                              DoubleCheckLocking doubleCheckLocking,
                              CacheDelayedConsumer cacheDelayedConsumer,
                              CacheDelayedProducer cacheDelayedProducer) {
        super(cacheConfigProperties.isCacheNullValues());
        this.cacheName = cacheName;
        this.cacheConfigProperties = cacheConfigProperties;
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.redisDistributedLock = redisDistributedLock;
        this.doubleCheckLocking = doubleCheckLocking;
        this.cacheDelayedConsumer = cacheDelayedConsumer;
        this.cacheDelayedProducer = cacheDelayedProducer;
        this.cacheNamePrefix = new StringJoiner(KEY_SEGMENTATION)
                .add(SpringUtil.getApplicationName())
                .add(CACHE_PREFIX)
                .add(cacheName) + KEY_SEGMENTATION;
        log.debug("创建缓存实例名:{},缓存key前缀:{}", cacheName, cacheNamePrefix);
        this.bloomFilter = redissonClient.getBloomFilter(cacheNamePrefix + "bloomFilter");
        bloomFilter.tryInit(100000L, 0.03);
    }


    /**
     * 批量从缓存中获取数据。首先从Caffeine缓存中获取，如果某些键没有命中，则进一步从Redis中获取。
     *
     * @param cacheKeys 缓存的key列表
     * @return 缓存的键值对
     */
    @Override
    public <V> Map<String, V> batchGet(List<String> cacheKeys) {
        log.debug("批量获取缓存数据，keys: {}", cacheKeys);

        Map<String, V> result = new HashMap<>();

        // 尝试从Caffeine缓存中获取
        Map<String, Object> fromCaffeine = caffeineCache.getAllPresent(cacheKeys);
        for (Map.Entry<String, Object> entry : fromCaffeine.entrySet()) {
            if (entry.getValue() != null) {
                try {
                    result.put(entry.getKey(), (V) entry.getValue());
                } catch (ClassCastException e) {
                    log.warn("类型转换失败，key: {}, value: {}", entry.getKey(), entry.getValue(), e);
                }
            }
        }

        // 过滤出没有在Caffeine中获取到的key
        List<String> missedKeys = cacheKeys.stream()
                .filter(key -> !result.containsKey(key))
                .collect(Collectors.toList());

        // 如果有未命中的key，则从Redis中获取
        if (!missedKeys.isEmpty()) {
            Map<String, V> redisValues = (Map<String, V>) redisTemplate.opsForValue().multiGet(missedKeys);
            result.putAll(redisValues);

            // 将从Redis中获取的数据放入Caffeine缓存
            caffeineCache.putAll(redisValues);

            log.info("从Redis中获取到{}个缓存数据", redisValues.size());
        }

        return result;
    }

    /**
     * 批量向缓存中放入数据。
     *
     * @param cacheMap 需要放入的键值对
     */
    @Override
    public <V> void batchPut(Map<String, V> cacheMap) {
        log.debug("批量向缓存中放入数据, 数据量: {}", cacheMap.size());

        // 向Caffeine缓存中放入数据
        caffeineCache.putAll(cacheMap);

        // 使用Redis事务向Redis中放入数据
        redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) {
                connection.multi();
                cacheMap.forEach((key, value) -> {
                    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                    RedisSerializer<V> redisSerializer = (RedisSerializer<V>) redisTemplate.getValueSerializer();
                    byte[] valueBytes = redisSerializer.serialize(value);
                    connection.set(keyBytes, valueBytes);
                });
                connection.exec();
                return null;
            }
        });

        // 将键添加到布隆过滤器中
        cacheMap.keySet().forEach(bloomFilter::add);
    }

    /**
     * 批量从缓存中移除数据。
     *
     * @param cacheKeys 需要移除的key列表
     */
    @Override
    public void batchEvict(Collection<String> cacheKeys) {
        log.debug("批量从缓存中移除数据，keys: {}", cacheKeys);

        // 从Caffeine缓存中移除数据
        caffeineCache.invalidateAll(cacheKeys);

        // 使用Redis事务确保原子性
        redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) {
                connection.multi();
                cacheKeys.forEach(key -> connection.del(key.getBytes(StandardCharsets.UTF_8)));
                connection.exec();
                return null;
            }
        });
    }


    /**
     * 根据原始key转换为完整的缓存key。
     *
     * @param key 原始key
     * @return 完整的缓存key
     */
    public String cacheKey(Object key) {
        log.trace("转换原始key '{}' 为完整的缓存key", key);

        return cacheNamePrefix + key.toString();
    }

    /**
     * 获取缓存名称。
     *
     * @return 缓存名称
     */
    @Override
    public String getName() {
        log.trace("获取缓存名称: {}", this.cacheName);
        return this.cacheName;
    }

    /**
     * 获取底层原生的缓存对象。此处，由于我们有Caffeine和Redis两层缓存，所以返回当前对象表示两层缓存。
     *
     * @return 当前缓存对象
     */
    @Override
    public Object getNativeCache() {
        log.trace("获取底层原生的缓存对象");
        return this;
    }

    /**
     * 获取缓存值。首先检查本地缓存，如果没有则检查Redis。
     * 如果Redis也没有，它将使用双重检查锁或分布式锁来加载数据。
     *
     * @param key         缓存key
     * @param valueLoader 用于加载数据的回调函数
     * @return 缓存值
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        String cacheKey = cacheKey(key);
        // Check with bloom filter first
        if (!bloomFilter.contains(cacheKey)) {
            log.debug("Bloom filter check: Key does not exist, key: {}", cacheKey);
            return null;
        }

        Object value = caffeineCache.getIfPresent(cacheKey);

        if (value != null) {
            log.debug("从Caffeine缓存中获取数据，key: {}", cacheKey);
            return (T) value;
        }

        if (doubleCheckLocking.isLocked(cacheKey)) {
            // 双重检查锁策略
            synchronized (doubleCheckLocking.getLock(cacheKey)) {
                value = caffeineCache.getIfPresent(cacheKey);
                if (value == null) {
                    value = loadFromRedisAndPutIntoCaffeine(cacheKey, valueLoader);
                }
            }
        } else {
            // Redis分布式锁策略
            RLock lock = redisDistributedLock.getLock(cacheKey);
            try {
                lock.lock();
                value = caffeineCache.getIfPresent(cacheKey);
                if (value == null) {
                    value = loadFromRedisAndPutIntoCaffeine(cacheKey, valueLoader);
                }
            } finally {
                lock.unlock();
            }
        }
        return (T) value;
    }

    /**
     * 从Redis加载数据并放入Caffeine缓存。
     *
     * @param cacheKey    缓存的key
     * @param valueLoader 用于加载数据的回调函数
     * @return 加载的值
     */
    private Object loadFromRedisAndPutIntoCaffeine(String cacheKey, Callable<?> valueLoader) {
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            try {
                value = valueLoader.call();
                if (value != null) {
                    redisTemplate.opsForValue().set(cacheKey, value);
                } else {
                    // 存储特殊的空值并设置过期时间
                    redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_VALUE_EXPIRE_TIME);
                }
                // Add to Bloom filter
                bloomFilter.add(cacheKey);
                caffeineCache.put(cacheKey, value);
            } catch (Exception e) {
                log.error("加载数据失败，key: {}", cacheKey, e);
                throw new RuntimeException("加载数据失败，key: " + cacheKey, e);
            }
        } else if (NULL_VALUE.equals(value)) {
            // 如果是特殊的空值，则返回null
            value = null;
        } else {
            caffeineCache.put(cacheKey, value);
        }
        return value;
    }

    /**
     * 向缓存中放入键值对。该方法将数据同时放入Caffeine缓存和Redis。
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    @Override
    public void put(Object key, Object value) {
        String cacheKey = cacheKey(key);

        // 向Caffeine缓存中放入数据
        caffeineCache.put(cacheKey, value);
        log.debug("向Caffeine缓存中放入数据，key: {}", cacheKey);

        // 向Redis中放入数据
        redisTemplate.opsForValue().set(cacheKey, value);

        // Add to Bloom filter
        bloomFilter.add(cacheKey);
        log.debug("向Redis缓存中放入数据，key: {}", cacheKey);
    }

    /**
     * 如果缓存中没有指定的键，则放入键值对。
     *
     * @param key   键
     * @param value 值
     * @return 之前与键关联的值，或null
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String cacheKey = cacheKey(key);
        Object existingValue = caffeineCache.getIfPresent(cacheKey);

        if (existingValue == null) {
            log.debug("缓存中未找到键：{}，放入新值", cacheKey);

            // 向Caffeine缓存中放入数据
            caffeineCache.put(cacheKey, value);
            // 向Redis中放入数据
            redisTemplate.opsForValue().setIfAbsent(cacheKey, value);

            return toValueWrapper(value);
        }

        log.debug("缓存中已存在键：{}，不放入新值", cacheKey);
        return toValueWrapper(existingValue);
    }


    /**
     * 从缓存中移除指定的key。
     *
     * @param key 需要移除的key
     */
    @Override
    public void evict(Object key) {
        String cacheKey = cacheKey(key);

        // 从Caffeine缓存中移除数据
        caffeineCache.invalidate(cacheKey);
        log.debug("从Caffeine缓存中移除数据，key: {}", cacheKey);

        // 从Redis中移除数据
        redisTemplate.delete(cacheKey);
        log.debug("从Redis缓存中移除数据，key: {}", cacheKey);
    }

    /**
     * 如果缓存中存在指定的键，则将其逐出。
     *
     * @param key 需要移除的键
     * @return 如果键被成功逐出返回true，否则返回false
     */
    @Override
    public boolean evictIfPresent(Object key) {
        String cacheKey = cacheKey(key);
        boolean evicted = false;

        // 检查Caffeine缓存中是否存在该键
        if (caffeineCache.getIfPresent(cacheKey) != null) {
            log.debug("从Caffeine缓存中逐出数据，key: {}", cacheKey);
            // 从Caffeine缓存中移除数据
            caffeineCache.invalidate(cacheKey);
            evicted = true;
        }

        // 检查Redis中是否存在该键
        if (redisTemplate.hasKey(cacheKey)) {
            log.debug("从Redis缓存中逐出数据，key: {}", cacheKey);
            // 从Redis中移除数据
            redisTemplate.delete(cacheKey);
            evicted = true;
        }

        if (!evicted) {
            log.debug("键未在任何缓存中找到，因此未执行逐出操作，key: {}", cacheKey);
        }

        return evicted;
    }

    /**
     * 清空缓存。
     */
    @Override
    public void clear() {
        // 清空Caffeine缓存
        caffeineCache.invalidateAll();
        log.debug("清空Caffeine缓存");

        // 注意：这里选择清空与当前缓存名称相关的Redis缓存，而不是整个Redis缓存
        String pattern = cacheNamePrefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("清空Redis缓存，keys: {}", keys);
        }
    }

    /**
     * 清空整个缓存，包括Caffeine缓存和Redis缓存。
     *
     * @return 如果缓存被成功清除返回true，否则返回false
     */
    @Override
    public boolean invalidate() {
        // 清除Caffeine缓存
        caffeineCache.invalidateAll();
        log.debug("清空Caffeine缓存");

        // 清除Redis缓存，这里使用前缀来匹配所有相关的键
        Set<String> keysToInvalidate = redisTemplate.keys(cacheNamePrefix + "*");
        if (keysToInvalidate != null && !keysToInvalidate.isEmpty()) {
            redisTemplate.delete(keysToInvalidate);
            log.debug("清空Redis缓存，keys: {}", keysToInvalidate);
            return true;
        }

        return false;
    }

    /**
     * 根据键检索缓存值。该方法不会触发任何数据加载或计算。
     *
     * @param key 要检索的键
     * @return 缓存中与键关联的值，如果键不存在则返回null
     */
    @Override
    protected Object lookup(Object key) {
        String cacheKey = cacheKey(key);

        // 从Caffeine缓存中检索
        // Check with bloom filter first
        if (!bloomFilter.contains(cacheKey)) {
            log.debug("Bloom filter check: Key does not exist, key: {}", cacheKey);
            return null;
        }

        Object value = caffeineCache.getIfPresent(cacheKey);
        if (value != null) {
            log.debug("从Caffeine缓存中检索到数据，key: {}", cacheKey);
            return value;
        }

        // 如果在Caffeine缓存中找不到，尝试从Redis中检索
        value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.debug("从Redis缓存中检索到数据，key: {}", cacheKey);
        } else {
            log.debug("在任何缓存中都找不到数据，key: {}", cacheKey);
        }
        return value;
    }

}



