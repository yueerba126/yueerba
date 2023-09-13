package com.yueerba.framework.cache.manage.config;

import com.yueerba.framework.cache.manage.support.RedisCaffeineCacheManager;
import com.yueerba.framework.cache.manage.sync.DoubleCheckLocking;
import com.yueerba.framework.cache.manage.sync.RedisDistributedLock;
import com.yueerba.framework.cache.properties.CacheConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.redisson.api.RedissonClient;

/**
 * Description:
 *
 * CacheConfig 负责为应用程序配置缓存基础设施。
 * 它集成了本地（Caffeine）和分布式（Redis）缓存的所有必要配置。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Configuration
public class CacheConfig {

    // 为缓存配置注入所需的依赖项

    /** 缓存配置属性 */
    @Autowired
    private CacheConfigProperties cacheConfigProperties;

    /** Redis模板，用于进行Redis操作 */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** Redisson客户端，用于分布式锁等操作 */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 创建并返回一个RedisCaffeineCacheManager实例。
     *
     * @return RedisCaffeineCacheManager的新实例
     */
    @Bean
    public RedisCaffeineCacheManager cacheManager() {
        // 初始化双重检查锁和Redis分布式锁策略
        DoubleCheckLocking doubleCheckLocking = new DoubleCheckLocking();
        RedisDistributedLock redisDistributedLock = new RedisDistributedLock(redissonClient);

        // 创建缓存管理器实例并返回
        return new RedisCaffeineCacheManager(
                cacheConfigProperties,
                redisTemplate,
                redissonClient,
                doubleCheckLocking,
                redisDistributedLock
        );
    }
}

