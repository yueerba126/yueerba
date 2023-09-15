package com.yueerba.framework.cache.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: 缓存总配置
 * Author: yueerba
 * Date: 2023/9/15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    /**
     * 缓存key的分隔符
     */
    public final static String KEY_SEGMENTATION = ":";

    /**
     * 缓存前缀
     */
    public final static String CACHE_PREFIX = "RedisCaffeineCache";

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean cacheNullValues = true;

    /**
     * 本地缓存异步驱逐延时时长（默认1秒）
     */
    private long cacheAsyExpelDelay = 1;

    /**
     * 每个cacheName的过期时间，单位毫秒，优先级比defaultExpiration高
     */
    private Map<String, Long> redisExpires = new HashMap<>();

    /**
     * 开始随机过期时间（默认7天）
     */
    private long timeOutBegin = 7 * 24 * 60 * 60;

    /**
     * 结束随机过期时间（默认30天）
     */
    private long timeOutEnd = 30 * 24 * 60 * 60;
}
