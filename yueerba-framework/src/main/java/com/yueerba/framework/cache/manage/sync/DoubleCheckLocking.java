package com.yueerba.framework.cache.manage.sync;

package com.sydata.framework.cache.sync;

import com.sydata.framework.cache.support.MultiCache;
import com.yueerba.framework.cache.manage.support.RedisCaffeineCache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 双重检查锁策略。
 * 该类提供了一种在本地环境中实现线程安全的策略，避免多个线程同时加载同一个key的数据。
 */
@Component
public class DoubleCheckLocking {

    /**
     * 用于存储每个key对应的锁。
     */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 缓存管理器。
     */
    private CacheManager cacheManager;

    /**
     * 根据给定的key获取对应的锁。如果锁不存在，它将被创建。
     *
     * @param key 缓存key
     * @return 对应的锁
     */
    public ReentrantLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * 检查给定的key是否被锁定。
     *
     * @param key 缓存key
     * @return 如果key被锁定返回true，否则返回false
     */
    public boolean isLocked(String key) {
        ReentrantLock lock = locks.get(key);
        return lock != null && lock.isLocked();
    }

    /**
     * 获取缓存管理器。
     *
     * @return 缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 设置缓存管理器。
     *
     * @param cacheManager 缓存管理器
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}

