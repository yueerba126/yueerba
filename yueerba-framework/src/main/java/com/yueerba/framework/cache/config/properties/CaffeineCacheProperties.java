package com.yueerba.framework.cache.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Description: Caffeine缓存属性配置类
 * Author: yueerba
 * Date: 2023/9/15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "caffeine.cache")
public class CaffeineCacheProperties {

    /**
     * Caffeine的配置描述符。
     * 示例：maximumSize=100,expireAfterWrite=30m
     */
    private String spec;

    /**
     * Caffeine缓存的最大大小。
     * 注意：此处默认设置为10240（10K条目）。
     */
    private long maximumSize = 1024 * 10;

    /**
     * 条目写入后的过期时间（毫秒）。
     * 注意：此处默认设置为1小时（3600000毫秒）。
     */
    private long expireAfterWrite = 1 * 60 * 60 * 1000;

    /**
     * 条目访问后的过期时间（毫秒）。
     * 注意：此处默认设置为1小时（3600000毫秒）。
     */
    private long expireAfterAccess = 1 * 60 * 60 * 1000;


    /**
     * Caffeine的初始容量。
     * 注意：默认初始容量为16
     */
    private long initialCapacity = 16;
}
