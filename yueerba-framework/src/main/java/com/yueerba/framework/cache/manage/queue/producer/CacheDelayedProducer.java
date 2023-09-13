package com.yueerba.framework.cache.manage.queue.producer;

import com.yueerba.framework.cache.manage.queue.CacheChange;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * CacheDelayedProducer 负责将缓存变化事件放入队列。
 * 这个类使用了一个延迟队列来存储缓存变化事件，并提供了一个方法来生产这些事件。
 */
@Slf4j
public class CacheDelayedProducer {

    /**
     * 用于存储缓存变化事件的队列。
     */
    private final BlockingQueue<CacheChange> queue;

    /**
     * 构造函数。
     */
    public CacheDelayedProducer() {
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * 将一个缓存变化事件放入队列。
     *
     * @param cacheChange 要放入队列的缓存变化事件
     */
    public void produce(CacheChange cacheChange) {
        try {
            queue.put(cacheChange);
            log.debug("成功将缓存变化事件放入队列, key: {}", cacheChange.getKey());
        } catch (InterruptedException e) {
            log.error("在尝试将缓存变化事件放入队列时发生错误", e);
        }
    }

    /**
     * 获取队列，这可以用于消费者来消费队列中的事件。
     *
     * @return 存储缓存变化事件的队列
     */
    public BlockingQueue<CacheChange> getQueue() {
        return queue;
    }
}
