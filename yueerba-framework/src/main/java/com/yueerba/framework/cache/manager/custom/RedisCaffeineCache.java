package com.yueerba.framework.cache.manager.custom;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.yueerba.framework.cache.batch.BatchOperationInterface;
import com.yueerba.framework.cache.lock.DoubleCheckLocking;
import com.yueerba.framework.cache.lock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.yueerba.framework.cache.config.properties.CacheProperties.CACHE_PREFIX;
import static com.yueerba.framework.cache.config.properties.CacheProperties.KEY_SEGMENTATION;

/**
 * Description: 自定义的Redis和Caffeine的组合缓存实现
 * Author: yueerba
 * Date: 2023/9/15
 */
@Slf4j
public class RedisCaffeineCache extends CaffeineCache implements BatchOperationInterface {

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
     * 特殊的空值用于解决缓存穿透问题
     */
    private static final String NULL_VALUE = "CUSTOM_NULL_VALUE";

    /**
     * 空值在Redis中的过期时间（秒）
     */
    private static final int NULL_VALUE_EXPIRE_TIME = 300;

    /**
     * 使用指定的名称、Caffeine缓存实例、RedisTemplate、RedissonClient、
     * RedisDistributedLock、DoubleCheckLocking、RBloomFilter、是否允许null值等属性
     * 创建一个新的RedisCaffeineCache实例。
     *
     * @param name 缓存的名称。
     * @param cache 用于本地存储的Caffeine缓存实例。
     * @param allowNullValues 是否允许缓存值为null。
     * @param redisTemplate Redis操作模板。
     * @param redissonClient Redisson客户端。
     * @param redisDistributedLock Redis分布式锁工具。
     * @param doubleCheckLocking 双重检查锁策略。
     * @param bloomFilter 用于缓存键的布隆过滤器。
     */
    public RedisCaffeineCache(String name, Cache<Object, Object> cache, boolean allowNullValues,
                              RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient,
                              RedisDistributedLock redisDistributedLock, DoubleCheckLocking doubleCheckLocking,
                              RBloomFilter<String> bloomFilter) {
        super(name, cache, allowNullValues);

        this.cacheName = name;
        this.cacheNamePrefix = new StringJoiner(KEY_SEGMENTATION)
                .add(SpringUtil.getApplicationName())
                .add(CACHE_PREFIX)
                .add(cacheName) + KEY_SEGMENTATION;
        log.debug("创建缓存实例名:{},缓存key前缀:{}", cacheName, cacheNamePrefix);

        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.redisDistributedLock = redisDistributedLock;
        this.doubleCheckLocking = doubleCheckLocking;

        // 布隆过滤器初始化
        this.bloomFilter = redissonClient.getBloomFilter(cacheNamePrefix + "bloomFilter");
        bloomFilter.tryInit(100000L, 0.03);

        log.debug("初始化RedisCaffeineCache实例，名称: {}, 是否允许null值: {}", name, allowNullValues);
    }



    /**
     * 获取缓存值，如果指定的键不存在，则通过提供的回调函数{@code valueLoader}加载并存储缓存值。
     *
     * @param key 缓存的键。
     * @param valueLoader 用于加载缓存值的回调函数。
     * @return 缓存的值，如果键不存在则通过{@code valueLoader}加载并存储后返回。
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.debug("尝试获取缓存值，键: {}", key);

        // 在Caffeine缓存中查找缓存值
        T value = (T) super.get(key);

        if (value == null) {
            log.debug("在Caffeine缓存中未找到值，尝试从Redis或通过加载器加载值，键: {}", key);

            // 尝试从Redis中检索缓存值
            // 如果存在，则将其存储到Caffeine缓存中
            // 如果不存在，则通过valueLoader加载值
            value = loadFromRedisOrLoadWithLoader(key, valueLoader);
        }

        log.debug("获取缓存值完成，键: {}，值: {}", key, value);
        return value;
    }


    /**
     * 向缓存中添加或更新指定的键值对。
     *
     * @param key   要添加或更新的键。
     * @param value 要添加或更新的值。
     */
    @Override
    public void put(Object key, Object value) {
        log.debug("尝试向缓存中添加或更新值，键: {}，值: {}", key, value);

        // 生成缓存键
        String cacheKey = cacheKey(key);

        // 如果值为null，将其设置为特殊的空值以解决缓存穿透问题
        if (value == null) {
            log.debug("值为null，将其设置为特殊的空值以解决缓存穿透问题，键: {}", cacheKey);
            redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_VALUE_EXPIRE_TIME);
        } else {
            // 否则，将值存储到Redis中
            log.debug("将值存储到Redis中，键: {}", cacheKey);
            redisTemplate.opsForValue().set(cacheKey, value);

            // 将缓存键添加到布隆过滤器中
            log.debug("将缓存键添加到布隆过滤器中，键: {}", cacheKey);
            bloomFilter.add(cacheKey);

            // 将值存储到Caffeine缓存中
            log.debug("将值存储到Caffeine缓存中，键: {}", key);
            super.put(key, value);
        }

        log.debug("添加或更新缓存值完成，键: {}，值: {}", key, value);
    }


    /**
     * 从缓存中删除指定的键及其关联的值。
     *
     * @param key 要删除的键。
     */
    @Override
    public void evict(Object key) {
        log.debug("尝试从缓存中删除值，键: {}", key);

        // 生成缓存键
        String cacheKey = cacheKey(key);

        // 从Redis中删除键值对
        log.debug("从Redis中删除键值对，键: {}", cacheKey);
        redisTemplate.delete(cacheKey);

        // 从Caffeine缓存中删除键值对
        log.debug("从Caffeine缓存中删除键值对，键: {}", key);
        super.evict(key);

        log.debug("删除缓存值完成，键: {}", key);
    }


    /**
     * 清空缓存，删除当前缓存中的所有键值对。
     */
    @Override
    public void clear() {
        log.debug("尝试清空缓存");

        // 清空Caffeine缓存
        log.debug("清空Caffeine缓存");
        super.clear();

        // 注意：这里选择清空与当前缓存名称相关的Redis缓存，而不是整个Redis缓存
        String pattern = cacheNamePrefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            // 从Redis中删除与当前缓存名称相关的所有键值对
            log.debug("从Redis中删除与当前缓存名称相关的所有键值对，keys: {}", keys);
            redisTemplate.delete(keys);
        }

        log.debug("清空缓存完成");
    }


    /**
     * 根据指定的键查找缓存值。
     *
     * @param key 要查找的键。
     * @return 缓存中与键关联的值，如果键不存在则返回 null。
     */
    @Override
    protected Object lookup(Object key) {
        log.debug("尝试查找缓存值，键: {}", key);

        // 生成缓存键
        String cacheKey = cacheKey(key);

        // 首先从Caffeine缓存中查找
        log.debug("尝试从Caffeine缓存中查找，键: {}", key);
        Object value = super.lookup(key);

        if (value != null) {
            log.debug("从Caffeine缓存中找到值，键: {}", key);
            return value;
        }

        // 如果在Caffeine缓存中找不到，尝试从Redis中查找
        log.debug("尝试从Redis缓存中查找，键: {}", cacheKey);
        value = redisTemplate.opsForValue().get(cacheKey);

        if (value != null) {
            log.debug("从Redis缓存中找到值，键: {}", cacheKey);
            CacheChange cacheChange = new CacheChange(CacheChange.EventType.LOOK, cacheKey, null);
            cacheDelayedConsumer.processCacheChange(cacheChange);
        } else {
            log.debug("在任何缓存中都找不到值，键: {}", key);
        }

        return value;
    }


    /**
     * 批量从缓存中获取数据。首先从Caffeine缓存中获取，如果某些键没有命中，则进一步从Redis中获取。
     *
     * @param cacheKeys 缓存的key列表
     * @return 缓存的键值对，其中键为缓存的key，值为对应的缓存值。
     */
    @Override
    public <V> Map<String, V> batchGet(List<String> cacheKeys) {
        log.debug("批量获取缓存数据，keys: {}", cacheKeys);

        // 用于存储获取到的缓存数据
        Map<String, V> result = new HashMap<>();

        // 尝试从Caffeine缓存中获取
        Map<String, Object> fromCaffeine = caffeineCache.getAllPresent(cacheKeys);
        for (Map.Entry<String, Object> entry : fromCaffeine.entrySet()) {
            if (entry.getValue() != null) {
                try {
                    result.put(entry.getKey(), (V) entry.getValue());
                    log.debug("从Caffeine缓存中获取数据，key: {}", entry.getKey());
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
     * @param map 需要放入的键值对，其中键为缓存的key，值为对应的缓存值。
     */
    @Override
    public <V> void batchPut(Map<String, V> map) {
        log.debug("批量向缓存中放入数据, 数据量: {}", map.size());

        // 向Caffeine缓存中放入数据
        caffeineCache.putAll(map);

        // 使用Redis事务向Redis中放入数据
        redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) {
                connection.multi();
                map.forEach((key, value) -> {
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
        map.keySet().forEach(bloomFilter::add);

        // 使用生产者将缓存变化放入队列
        map.forEach((key, value) -> {
            CacheChange cacheChange = new CacheChange(CacheChange.EventType.ADD, key, value);
            cacheDelayedProducer.produce(cacheChange);
        });
    }


    /**
     * 批量从缓存中移除数据。
     *
     * @param cacheKeys 需要移除的key列表，这些key对应着要从缓存中移除的数据。
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

        // 使用生产者将缓存删除操作放入队列
        cacheKeys.forEach(key -> {
            // 为每个缓存键创建删除事件
            CacheChange cacheChange = new CacheChange(CacheChange.EventType.DELETE, key, null);
            cacheDelayedProducer.produce(cacheChange);
        });
    }


    /**
     * 根据原始 key 转换为完整的缓存 key。
     *
     * @param key 原始 key
     * @return 完整的缓存 key
     */
    @Override
    public String cacheKey(Object key) {
        log.trace("将原始 key '{}' 转换为完整的缓存 key", key);

        // 在这里，根据原始 key 的规则生成完整的缓存 key
        // 这个方法用于将应用程序中的原始 key 映射到实际用于缓存的 key，以确保缓存数据的唯一性和正确性。

        return cacheNamePrefix + key.toString();
    }



    /**
     * 从Redis中检索缓存值，如果Redis中不存在，则通过提供的回调函数 {@code valueLoader} 加载值，
     * 并将其存储到Redis和Caffeine缓存中。
     *
     * @param key 缓存的键。
     * @param valueLoader 用于加载缓存值的回调函数。
     * @return 缓存的值，如果无法加载则返回null。
     */
    private <T> T loadFromRedisOrLoadWithLoader(Object key, Callable<T> valueLoader) {
        log.debug("尝试从Redis中检索缓存值，键: {}", key);

        String cacheKey = cacheKey(key);
        // 使用布隆过滤器检查缓存键是否存在
        if (!bloomFilter.contains(cacheKey)) {
            log.debug("Bloom filter检查: 缓存键不存在于布隆过滤器中，键: {}", cacheKey);

            // 尝试获取Redis分布式锁
            RLock lock = redisDistributedLock.getLock(cacheKey);
            try {
                lock.lock();
                // 重新检查布隆过滤器以确保在获取锁之前没有其他线程加载缓存
                if (!bloomFilter.contains(cacheKey)) {
                    log.debug("获取了Redis分布式锁并重新检查布隆过滤器: 缓存键不存在于布隆过滤器中，键: {}", cacheKey);

                    // 从Redis中获取缓存值
                    T value = (T) redisTemplate.opsForValue().get(cacheKey);
                    if (value == null) {
                        log.debug("在Redis中未找到缓存值，通过加载器加载缓存值，键: {}", cacheKey);

                        // 使用提供的回调函数加载缓存值
                        try {
                            value = valueLoader.call();
                            if (value != null) {
                                // 存储缓存值到Redis
                                redisTemplate.opsForValue().set(cacheKey, value);
                                log.debug("将缓存值存储到Redis，键: {}", cacheKey);
                            } else {
                                // 存储特殊的空值以解决缓存穿透问题
                                redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_VALUE_EXPIRE_TIME);
                                log.debug("存储特殊的空值到Redis以解决缓存穿透问题，键: {}", cacheKey);
                            }

                            // 将缓存键添加到布隆过滤器中
                            bloomFilter.add(cacheKey);
                            log.debug("将缓存键添加到布隆过滤器中，键: {}", cacheKey);

                            // 存储缓存值到Caffeine缓存
                            super.put(key, value);
                            log.debug("将缓存值存储到Caffeine缓存，键: {}", key);
                        } catch (Exception e) {
                            log.error("加载缓存值失败，键: {}", cacheKey, e);
                            throw new RuntimeException("加载缓存值失败，键: " + cacheKey, e);
                        }
                    } else if (NULL_VALUE.equals(value)) {
                        // 如果是特殊的空值，则返回null
                        value = null;
                    } else {
                        // 存储缓存值到Caffeine缓存
                        super.put(key, value);
                        log.debug("从Redis中获取到缓存值并存储到Caffeine缓存，键: {}", key);
                    }
                } else {
                    log.debug("Redis分布式锁获取后，缓存键已存在于布隆过滤器中，可能有其他线程加载，键: {}", cacheKey);
                }
            } finally {
                lock.unlock();
            }
        } else {
            log.debug("缓存键已存在于布隆过滤器中，可能有其他线程加载，键: {}", cacheKey);
        }

        // 获取或重新加载缓存值后返回
        T cachedValue = (T) super.get(key);
        log.debug("加载或重新加载缓存值完成，键: {}，值: {}", key, cachedValue);
        return cachedValue;
    }

}
