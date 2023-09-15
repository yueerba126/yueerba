package com.yueerba.framework.cache.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置属性类。
 * <p>
 * 用于封装与Redisson相关的配置信息，支持集群和哨兵模式。
 * </p>
 *
 * @author yueerba
 * @date 2023/9/15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis.redisson")
public class RedissonProperties {
    /**
     * 线程数
     */
    private int threads = 16;

    /**
     * netty线程数
     */
    private int nettyThreads = 32;
}

