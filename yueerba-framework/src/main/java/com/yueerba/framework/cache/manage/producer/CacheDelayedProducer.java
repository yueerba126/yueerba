package com.yueerba.framework.cache.manage.producer;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用于延迟同步策略的生产者类。
 * 当数据被写入到Caffeine缓存但还没有被同步到Redis时，
 * 我们可以使用此类将这些待同步的键添加到队列中。
 */
@Component
public class CacheDelayedProducer {

    /**
     * 使用一个线程安全的队列来保存待同步的键。
     * 这个队列将被CacheDelayedConsumer消费，来执行实际的同步操作。
     */
    private final ConcurrentLinkedQueue<String> delayedSyncKeys = new ConcurrentLinkedQueue<>();

    /**
     * 当数据被写入Caffeine缓存但还没有被同步到Redis时，调用此方法。
     * 这个方法将键添加到内部的队列中，等待后续的同步操作。
     *
     * @param key 待同步的键
     */
    public void addToDelayedSync(String key) {
        delayedSyncKeys.offer(key);
    }

    /**
     * 从队列中获取并移除首个待同步的键。
     * 如果队列为空，则返回 null。
     *
     * @return 待同步的键或null
     */
    public String poll() {
        return delayedSyncKeys.poll();
    }
}

