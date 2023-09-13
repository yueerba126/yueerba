package com.yueerba.framework.cache.config;

import com.yueerba.framework.cache.config.extend.RedissonAutoConfigurationCustomizer;
import com.yueerba.framework.cache.config.selector.RedisConfigSelector;
import com.yueerba.framework.cache.serializer.GenericJackson2JsonCodec;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisOperations;

import java.util.List;

/**
 * Description:
 *
 * RedissonAutoConfiguration 类负责自动配置 Redisson 客户端。
 * 它基于应用的 Redis 配置来创建和初始化 RedissonClient。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Configuration
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonAutoConfiguration {

    // Redis 的相关属性，通常来源于应用的配置文件
    @Autowired
    private RedisProperties redisProperties;

    // 用于选择和构建 Redis 配置的组件
    @Autowired
    private RedisConfigSelector configSelector;

    // 用于自定义 Redisson 配置的组件列表
    @Autowired(required = false)
    private List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;

    // Redis 的编解码器，用于序列化和反序列化 Redis 的值
    @Autowired
    private GenericJackson2JsonCodec genericJackson2JsonCodec;

    /**
     * 创建和初始化 RedissonClient Bean。
     *
     * @return 初始化后的 RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redisson() {
        // 使用 RedisConfigSelector 选择和构建相应的 Redis 配置
        Config config = configSelector.selectConfig();

        // 应用所有的自定义配置
        applyCustomizations(config);

        // 设置编解码器
        config.setCodec(genericJackson2JsonCodec);

        // 创建和返回 RedissonClient
        return Redisson.create(config);
    }

    /**
     * 应用所有的自定义配置到给定的 Config 对象。
     *
     * @param config 要定制的 Redisson Config 对象
     */
    private void applyCustomizations(Config config) {
        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
    }
}

