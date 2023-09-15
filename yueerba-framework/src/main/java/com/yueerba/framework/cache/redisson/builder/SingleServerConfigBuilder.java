package com.yueerba.framework.cache.redisson.builder;

import com.yueerba.framework.cache.utils.NodeConverter;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Description:
 *
 * SingleServerConfigBuilder 类负责构建 Redis 单服务器模式的配置。
 * 该类依赖 RedisProperties 来获取单服务器的相关属性，并创建相应的 Redisson Config 对象。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Component
public class SingleServerConfigBuilder {

    // Redis 的相关属性，通常来源于应用的配置文件
    @Resource
    private RedisProperties redisProperties;

    /**
     * 根据 RedisProperties 构建 Redis 单服务器模式的配置。
     *
     * @param properties 应用配置中的 Redis 属性
     * @return 针对 Redis 单服务器模式的 Redisson Config 对象
     */
    public Config build(RedisProperties properties) {
        // 创建一个新的 Redisson 配置对象
        Config config = new Config();

        // 获取单服务器的地址
        String address = getServerAddress(properties);

        // 设置单服务器配置
        config.useSingleServer()
                .setAddress(address)
                .setConnectTimeout(getTimeout(properties))
                .setDatabase(properties.getDatabase())
                .setPassword(properties.getPassword());

        return config;
    }

    /**
     * 从 RedisProperties 中获取单服务器的地址。
     *
     * @param properties 应用配置中的 Redis 属性
     * @return 单服务器的地址，形如 "redis://host:port"
     */
    private String getServerAddress(RedisProperties properties) {
        // 确定协议前缀
        String prefix = properties.isSsl() ? NodeConverter.REDISS_PROTOCOL_PREFIX : NodeConverter.REDIS_PROTOCOL_PREFIX;

        // 返回完整的服务器地址
        return prefix + properties.getHost() + ":" + properties.getPort();
    }

    /**
     * 从 RedisProperties 中获取连接超时时间。
     *
     * @param properties 应用配置中的 Redis 属性
     * @return 连接超时时间，以毫秒为单位
     */
    private int getTimeout(RedisProperties properties) {
        // 获取超时时间，以毫秒为单位
        long timeoutMillis = properties.getTimeout().toMillis();

        // 确保超时时间不超过 Integer.MAX_VALUE
        if (timeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalStateException("Redis timeout is too large to be represented as an int.");
        }

        // 确保超时时间不为负
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Redis timeout cannot be negative.");
        }

        return (int) timeoutMillis;
    }
}

