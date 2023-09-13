package com.yueerba.framework.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Description:
 *
 * RedisKeyspaceEventListener 监听 Redis 的键空间事件。
 * 当 Redis 中的缓存条目发生特定的事件（例如条目过期、条目被删除等）时，该监听器将被触发。
 * 此类的目的是将 Redis 的键空间事件转换为通用的缓存事件，然后通知注册的 CacheEventListener。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Slf4j
public class RedisKeyspaceEventListener implements MessageListener {

    /**
     * 用于将 Redis 消息转换为对象的 RedisTemplate 实例。
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 注册的缓存事件监听器，它将被通知当缓存事件发生。
     */
    private final CacheEventListener cacheEventListener;

    /**
     * 使用给定的 RedisTemplate 和 CacheEventListener 构造 RedisKeyspaceEventListener。
     *
     * @param redisTemplate      用于将 Redis 消息转换为对象的 RedisTemplate 实例。
     * @param cacheEventListener 注册的缓存事件监听器。
     */
    public RedisKeyspaceEventListener(RedisTemplate<String, Object> redisTemplate, CacheEventListener cacheEventListener) {
        this.redisTemplate = redisTemplate;
        this.cacheEventListener = cacheEventListener;
    }

    /**
     * 当接收到 Redis 消息时触发。
     *
     * @param message Redis 发出的消息。
     * @param pattern 订阅的模式。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 解析消息体，获取动作类型
        String actionType = new String(message.getBody());

        // 解析频道名称以获取数据库索引和事件类型
        String channel = new String(message.getChannel());
        String[] parts = channel.split(":");
        String dbIndex = parts[1].replaceAll("[^0-9]", ""); // 提取数字部分，即数据库索引
        String action = parts[2]; // 获取真实的事件动作，例如 set, del, expired 等

        String key = redisTemplate.getKeySerializer().deserialize(message.getChannel()).toString();

        // 根据收到的 action 判断 Redis 键事件的类型，并进行相应处理。
        switch (action) {
            // 当 Redis 中有一个新键值对被设置时
            case "set":
                // 从 Redis 中获取与该键关联的值
                Object valueOnSet = redisTemplate.opsForValue().get(key);
                // 通知监听器，一个新的键值对已被加入
                cacheEventListener.onPut(key, valueOnSet);
                break;

            // 当 Redis 中的键值对被删除时
            case "del":
                // 通知监听器，一个键值对已被删除
                cacheEventListener.onEvict(key);
                break;

            // 当 Redis 中的键值对到达其生命周期末尾并过期时
            case "expired":
                // 通知监听器，一个键值对已过期
                cacheEventListener.onExpire(key);
                break;

            // 当 Redis 中的键被重命名（旧键名）时
            case "rename_from":
                // 通知监听器，一个键已被重命名（这是旧的键名）
                cacheEventListener.onRenameFrom(key);
                break;

            // 当 Redis 中的键被重命名（新键名）时
            case "rename_to":
                // 从 Redis 中获取与新键关联的值
                Object valueOnRename = redisTemplate.opsForValue().get(key);
                // 通知监听器，一个键已被重命名（这是新的键名）
                cacheEventListener.onRenameTo(key, valueOnRename);
                break;

            // 当 Redis 数据库被清空时或所有数据库被清空时
            case "flushdb":
            case "flushall":
                // 通知监听器，缓存已被清空
                cacheEventListener.onClear();
                break;

            // 如果我们收到了未知的 Redis 键事件
            default:
                String errorMessage = "Received an unknown Redis key event: " + action;
                log.warn(errorMessage);
                // 可以选择执行一些默认的处理策略，例如记录错误并继续执行
                cacheEventListener.onError(new Exception(errorMessage));
                break;
        }
    }

}


