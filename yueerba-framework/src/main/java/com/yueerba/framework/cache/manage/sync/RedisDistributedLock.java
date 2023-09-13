package com.yueerba.framework.cache.manage.sync;

package com.sydata.framework.cache.sync;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁策略。
 * 使用Redisson客户端提供的分布式锁实现，确保在分布式环境中的线程安全。
 */
@Component
public class RedisDistributedLock {

    /**
     * Redisson客户端，用于操作Redis。
     */
    private final RedissonClient redissonClient;

    /**
     * 使用Redisson客户端进行初始化。
     *
     * @param redissonClient Redisson客户端
     */
    @Autowired
    public RedisDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取分布式锁。
     *
     * @param lockKey 锁的key
     * @return 分布式锁
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey 锁的key
     * @param waitTime 最长等待时间
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return 如果获取到锁返回true，否则返回false
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException("尝试获取Redis分布式锁时出错", e);
        }
    }
}

