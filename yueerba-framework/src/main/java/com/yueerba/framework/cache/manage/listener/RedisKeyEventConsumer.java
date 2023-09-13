package com.yueerba.framework.cache.manage.listener;

import com.yueerba.framework.cache.manage.support.RedisCaffeineCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis键空间事件的消费者。
 * 当Redis键过期或被删除时，此消费者负责处理相应的事件。
 */
@Component
public class RedisKeyEventConsumer extends KeyExpirationEventMessageListener {

    /**
     * Redis和Caffeine双层缓存的实现。
     * 用于同步Redis和Caffeine之间的缓存状态。
     */
    private final RedisCaffeineCache redisCaffeineCache;

    /**
     * 使用RedisCaffeineCache进行初始化。
     *
     * @param redisCaffeineCache Redis和Caffeine双层缓存的实现
     * @param listenerContainer  Redis消息监听容器
     */
    @Autowired
    public RedisKeyEventConsumer(RedisCaffeineCache redisCaffeineCache, RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
        this.redisCaffeineCache = redisCaffeineCache;
    }

    /**
     * 当接收到键过期或被删除的事件时，调用此方法。
     *
     * @param message Redis消息
     * @param pattern 消息匹配模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 获取失效的key
        String expiredKey = message.toString();

        // 从Caffeine缓存中移除该key
        redisCaffeineCache.evict(expiredKey);

        // 打印日志
        System.out.println("Received Redis key expiration event. Key: " + expiredKey);
    }
}

