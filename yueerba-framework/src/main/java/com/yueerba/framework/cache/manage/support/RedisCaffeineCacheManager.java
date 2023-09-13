package com.yueerba.framework.cache.manage.support;

import com.yueerba.framework.cache.manage.queue.consumer.CacheDelayedConsumer;
import com.yueerba.framework.cache.manage.queue.producer.CacheDelayedProducer;
import com.yueerba.framework.cache.manage.sync.DoubleCheckLocking;
import com.yueerba.framework.cache.manage.sync.RedisDistributedLock;
import com.yueerba.framework.cache.properties.CacheConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 *
 * RedisCaffeineCacheManager 是一个缓存管理器，它整合了Redis和Caffeine缓存，
 * 并使用策略模式来决定如何进行缓存操作，使用事件监听器模式来处理缓存相关的事件。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Component
public class RedisCaffeineCacheManager implements CacheManager {

    /**
     * 缓存配置属性
     */
    @Autowired
    private CacheConfigProperties cacheConfigProperties;

    /**
     * 用于存储多个RedisCaffeineCache实例的Map
     */
    private final Map<String, RedisCaffeineCache> cacheMap = new ConcurrentHashMap<>();

    /**
     * 根据缓存名称获取相应的RedisCaffeineCache实例。
     * 如果该名称的缓存不存在，则创建一个新的实例。
     *
     * @param name 缓存名称
     * @return 对应的RedisCaffeineCache实例
     */
    @Override
    public RedisCaffeineCache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    /**
     * 获取所有缓存的名称。
     *
     * @return 缓存名称集合
     */
    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    /**
     * 创建一个新的RedisCaffeineCache实例。
     *
     * @param name 缓存名称
     * @return 新的RedisCaffeineCache实例
     */
    private RedisCaffeineCache createCache(String name) {
        CacheConfigProperties.CaffeineConfig caffeineConfig = cacheConfigProperties.getCaffeine();

        // 创建Caffeine缓存实例
        com.github.benmanes.caffeine.cache.Cache<String, Object> caffeineCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(caffeineConfig.getMaximumSize())
                .expireAfterWrite(Duration.ofSeconds(caffeineConfig.getExpireAfterWrite()))
                .build();

        // 创建双重检查锁和Redis分布式锁实例
        DoubleCheckLocking doubleCheckLocking = new DoubleCheckLocking();
        RedisDistributedLock redisDistributedLock = new RedisDistributedLock();

        // 创建延迟同步策略实例
        CacheDelayedProducer cacheDelayedProducer = new CacheDelayedProducer();
        CacheDelayedConsumer cacheDelayedConsumer = new CacheDelayedConsumer(cacheDelayedProducer);

        // 创建新的RedisCaffeineCache实例
        return new RedisCaffeineCache(name, cacheConfigProperties, caffeineCache, doubleCheckLocking, redisDistributedLock, cacheDelayedConsumer::consume);
    }
}



