package com.yueerba.framework.cache.manage.queue.consumer;

import com.yueerba.framework.cache.manage.queue.CacheChange;
import com.yueerba.framework.cache.manage.queue.producer.CacheDelayedProducer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * CacheDelayedConsumer 负责从队列中消费缓存变化事件。
 * 这个类可以手动触发或定时触发，以处理队列中的事件。
 */
@Slf4j
public class CacheDelayedConsumer implements Runnable {

    /**
     * 从中消费缓存变化事件的队列。
     */
    private final BlockingQueue<CacheChange> queue;

    /**
     * 构造函数。
     *
     * @param producer CacheDelayedProducer的实例，用于获取队列
     */
    public CacheDelayedConsumer(CacheDelayedProducer producer) {
        this.queue = producer.getQueue();
    }

    /**
     * 手动消费队列中的一个事件。
     */
    public void consume() {
        try {
            CacheChange cacheChange = queue.take();
            processCacheChange(cacheChange);
        } catch (InterruptedException e) {
            log.error("在尝试从队列中消费缓存变化事件时发生错误", e);
        }
    }

    /**
     * 处理缓存变化事件。
     *
     * @param cacheChange 要处理的缓存变化事件
     */
    private void processCacheChange(CacheChange cacheChange) {
        // 在这里处理缓存变化事件，例如同步缓存、更新数据库等
        log.debug("处理缓存变化事件, key: {}", cacheChange.getKey());
    }

    /**
     * 定时消费队列中的事件。
     */
    @Override
    public void run() {
        while (true) {
            try {
                CacheChange cacheChange = queue.poll(1, TimeUnit.SECONDS);
                if (cacheChange != null) {
                    processCacheChange(cacheChange);
                }
            } catch (InterruptedException e) {
                log.error("在尝试从队列中消费缓存变化事件时发生错误", e);
            }
        }
    }
}
