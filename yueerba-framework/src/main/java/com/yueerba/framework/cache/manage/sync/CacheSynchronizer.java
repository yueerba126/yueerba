package com.yueerba.framework.cache.manage.sync;

import com.yueerba.framework.cache.manage.consumer.CacheDelayedConsumer;
import com.yueerba.framework.cache.manage.producer.CacheDelayedProducer;
import com.yueerba.framework.cache.manage.support.RedisCaffeineCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 缓存同步策略。
 * 该类的主要职责是协调Caffeine和Redis之间的缓存同步。
 */
@Slf4j
public class CacheSynchronizer {

    /**
     * 双重检查锁策略
     */
    @Autowired
    private DoubleCheckLocking doubleCheckLocking;

    /**
     * Redis分布式锁策略
     */
    @Autowired
    private RedisDistributedLock redisDistributedLock;

    /**
     * 缓存延迟同步生产者
     */
    @Autowired
    private CacheDelayedProducer cacheDelayedProducer;

    /**
     * 缓存延迟同步消费者
     */
    @Autowired
    private CacheDelayedConsumer cacheDelayedConsumer;

    /**
     * 将指定的键从本地缓存同步到Redis。
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void syncToRedis(String cacheName, Object key) {
        RedisCaffeineCache cache = (RedisCaffeineCache) doubleCheckLocking.getCacheManager().getCache(cacheName);
        Object value = cache.get(key);
        cache.put(key, value);
        log.debug("将键 {} 从本地缓存同步到Redis。", key);
    }

    /**
     * 将指定的键从Redis同步到本地缓存。
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void syncToLocal(String cacheName, Object key) {
        RedisCaffeineCache cache = (RedisCaffeineCache) redisDistributedLock.getCacheManager().getCache(cacheName);
        cache.evict(key);
        cache.get(key);
        log.debug("将键 {} 从Redis同步到本地缓存。", key);
    }

    /**
     * 将指定的键进行延迟同步。
     * 这可以用于减少在高并发环境下的同步操作。
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void delaySync(String cacheName, Object key) {
        cacheDelayedProducer.produce(cacheName, key);
        log.debug("将键 {} 进行了延迟同步。", key);
    }

    /**
     * 执行延迟同步的任务。
     * 通常由定时任务或后台线程调用。
     */
    public void executeDelayedSync() {
        cacheDelayedConsumer.consume();
        log.debug("执行了延迟同步任务。");
    }
}

