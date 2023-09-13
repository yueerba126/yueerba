package com.yueerba.framework.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Description: redisson配置类
 * Author: yueerba
 * Date: 2023/9/12
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
