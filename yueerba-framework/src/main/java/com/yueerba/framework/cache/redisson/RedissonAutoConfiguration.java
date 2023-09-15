package com.yueerba.framework.cache.redisson;

import com.yueerba.framework.cache.config.properties.RedissonProperties;
import com.yueerba.framework.cache.redisson.custom.RedissonAutoConfigurationCustomizer;
import com.yueerba.framework.cache.redisson.selector.RedisConfigSelector;
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

import javax.annotation.Resource;
import java.util.List;

/**
 * Description:
 * <p>
 * RedissonAutoConfiguration 类负责自动配置 Redisson 客户端。
 * 它基于应用的 Redis 配置来创建和初始化 RedissonClient。
 * <p>
 * Author: yueerba
 * Date: 2023/9/12
 */
@Configuration
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonAutoConfiguration {

    // Redis 的相关属性，通常来源于应用的配置文件
    @Resource
    private RedisProperties redisProperties;

    @Resource
    private RedissonProperties redissonProperties;

    // 用于选择和构建 Redis 配置的组件
    @Resource
    private RedisConfigSelector configSelector;

    // Redis 的编解码器，用于序列化和反序列化 Redis 的值
    @Resource
    private GenericJackson2JsonCodec genericJackson2JsonCodec;

    // 用于自定义 Redisson 配置的组件列表
    @Autowired(required = false)
    private List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;

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

        // 设置 Redisson 的内部任务执行线程数。
        // 这些线程会处理发布/订阅的消息、定时任务、远程服务调用等。
        // 如果不设置，默认会使用系统的处理器数量。
        config.setThreads(redissonProperties.getThreads());

        // 设置处理 Redis 服务器连接的 Netty 线程数。
        // 这些线程主要负责处理网络交互。
        // 如果不设置，默认会使用系统的处理器数量。
        config.setNettyThreads(redissonProperties.getNettyThreads());

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

