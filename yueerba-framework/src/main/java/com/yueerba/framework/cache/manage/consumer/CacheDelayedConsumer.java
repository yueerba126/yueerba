package com.yueerba.framework.cache.manage.consumer;

import com.yueerba.framework.cache.manage.producer.CacheDelayedProducer;
import com.yueerba.framework.cache.manage.support.RedisCaffeineCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存的延迟同步消费者类。
 * 该类定期从CacheDelayedProducer队列中取出待同步的键，并执行同步操作。
 */
@Component
public class CacheDelayedConsumer {

    /**
     * 注入CacheDelayedProducer，从中获取待同步的键。
     */
    @Autowired
    private CacheDelayedProducer cacheDelayedProducer;

    /**
     * 注入RedisCaffeineCache，用于实际的同步操作。
     */
    @Autowired
    private RedisCaffeineCache redisCaffeineCache;

    /**
     * 定期执行的方法，从队列中取出待同步的键，并执行同步操作。
     * 这里我们使用了Spring的@Scheduled注解，每隔5秒执行一次。
     * 你可以根据实际需求调整执行的频率。
     */
    @Scheduled(fixedRate = 5000)
    public void consume() {
        String keyToSync;
        while ((keyToSync = cacheDelayedProducer.poll()) != null) {
            syncKeyToRedis(keyToSync);
        }
    }

    /**
     * 将指定的键从Caffeine缓存同步到Redis。
     *
     * @param keyToSync 需要同步的键
     */
    private void syncKeyToRedis(String keyToSync) {
        Object value = redisCaffeineCache.getNativeCache().get(keyToSync);
        if (value != null) {
            redisCaffeineCache.put(keyToSync, value);
        }
    }
}

