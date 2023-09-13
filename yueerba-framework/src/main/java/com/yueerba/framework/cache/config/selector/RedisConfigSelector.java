package com.yueerba.framework.cache.config.selector;

import com.yueerba.framework.cache.config.builder.ClusterConfigBuilder;
import com.yueerba.framework.cache.config.builder.SentinelConfigBuilder;
import com.yueerba.framework.cache.config.builder.SingleServerConfigBuilder;
import com.yueerba.framework.cache.utils.ReflectionUtil;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;

/**
 * Description:
 *
 * RedisConfigSelector 类根据 RedisProperties 选择和构建相应的 Redis 配置。
 * 它根据 Redis 的部署模式（单实例、集群、哨兵）来决定使用哪个配置构建器。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Component
public class RedisConfigSelector {

    // 对应 Redis 的单实例模式配置构建器
    @Autowired
    private SingleServerConfigBuilder singleServerBuilder;

    // 对应 Redis 的集群模式配置构建器
    @Autowired
    private ClusterConfigBuilder clusterBuilder;

    // 对应 Redis 的哨兵模式配置构建器
    @Autowired
    private SentinelConfigBuilder sentinelBuilder;

    // Redis 的相关属性，通常来源于应用的配置文件
    @Autowired
    private RedisProperties redisProperties;

    /**
     * 根据 RedisProperties 选择并构建相应的 Redis 配置。
     *
     * @return 为 Redisson 提供的 Config 对象
     */
    public Config selectConfig() {
        // 如果是哨兵模式
        if (redisProperties.getSentinel() != null) {
            return sentinelBuilder.build(redisProperties);
        }
        // 如果是集群模式
        else if (isClusterConfig()) {
            return clusterBuilder.build(redisProperties);
        }
        // 默认为单实例模式
        else {
            return singleServerBuilder.build(redisProperties);
        }
    }

    /**
     * 判断是否为集群模式。
     *
     * @return 如果 RedisProperties 表示集群配置，则返回 true，否则返回 false。
     */
    private boolean isClusterConfig() {
        // 此处使用反射来检查是否为集群模式
        return ReflectionUtil.invokeMethod(RedisProperties.class, "getCluster", redisProperties) != null;
    }
}

