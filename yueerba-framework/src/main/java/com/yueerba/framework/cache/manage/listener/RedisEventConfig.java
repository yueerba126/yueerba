package com.yueerba.framework.cache.manage.listener;

package com.sydata.framework.cache.listener;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.event.RedissonSpringCacheEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis事件配置。
 * 该配置类主要处理Redis键空间通知事件的配置和监听。
 */
@Configuration
public class RedisEventConfig {

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
    public RedisEventConfig(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 配置Redis消息监听容器。
     * 该容器负责监听Redis的键空间通知。
     *
     * @return Redis消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redissonClient.getConfig().useSingleServer().getRedisClient().getRedisConnection().getRedisConnectionFactory());

        Map<MessageListener, Topic> listeners = new HashMap<>();
        // 添加一个监听器，监听所有键空间的通知
        listeners.put(redisKeyEventMessageListener(), new PatternTopic("__keyevent@*__:*"));
        container.setMessageListeners(listeners);

        return container;
    }

    /**
     * Redis键事件消息监听器。
     * 当键事件被触发时，此监听器将被调用。
     *
     * @return 消息监听器
     */
    @Bean
    public MessageListener redisKeyEventMessageListener() {
        return new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                // 处理接收到的键事件消息
                String channel = new String(message.getChannel());
                String body = new String(message.getBody());
                System.out.println("Received Redis event - Channel: " + channel + ", Body: " + body);
            }
        };
    }

    /**
     * 监听Redisson缓存事件。
     * 当缓存事件被触发时，此监听器将被调用。
     *
     * @param event Redisson缓存事件
     */
    @EventListener
    public void handleRedissonSpringCacheEvent(RedissonSpringCacheEvent event) {
        // 处理接收到的Redisson缓存事件
        System.out.println("Received Redisson event: " + event);
    }
}

