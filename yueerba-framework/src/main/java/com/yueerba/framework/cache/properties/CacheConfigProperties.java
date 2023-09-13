package com.yueerba.framework.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: 缓存模块全局配置
 * Author: yueerba
 * Date: 2023/9/12
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "redis.caffeine.cache")
public class CacheConfigProperties {

    /**
     * 缓存key的分隔符
     */
    public final static String KEY_SEGMENTATION = ":";

    /**
     * 缓存前缀
     */
    public final static String CACHE_PREFIX = "RedisCaffeineCache";

    /**
     * 是否存储空值，默认为true。这可以防止缓存穿透。
     */
    private boolean cacheNullValues = true;

    /**
     * Caffeine配置类
     */
    private CaffeineConfig caffeine = new CaffeineConfig();

    /**
     * Redis配置类
     */
    private RedisConfig redis = new RedisConfig();

    @Data
    public static class CaffeineConfig {
        /**
         * 本地缓存异步驱逐的延迟时间，单位：秒（默认1秒）
         */
        private long asyExpelDelay = 1;

        /**
         * 本地缓存在最后一次访问后的过期时间，单位：秒（默认1小时）
         */
        private long expireAfterAccess = 3600;

        /**
         * 本地缓存在写入后的过期时间，单位：秒（默认1小时）
         */
        private long expireAfterWrite = 3600;

        /**
         * 本地缓存的初始容量（默认20）
         */
        private int initialCapacity = 20;

        /**
         * 本地缓存的最大容量，超过这个数量时，之前的缓存将会被逐出（默认10240）
         */
        private int maximumSize = 10240;
    }

    @Data
    public static class RedisConfig {
        /**
         * 指定每个cacheName的过期时间，单位：秒。这个配置的优先级高于默认的过期时间。
         */
        private Map<String, Long> expires = new HashMap<>();

        /**
         * 随机过期时间的开始范围，单位：秒（默认7天）
         */
        private long timeOutBegin = 604800;

        /**
         * 随机过期时间的结束范围，单位：秒（默认30天）
         */
        private long timeOutEnd = 2592000;
    }
}


