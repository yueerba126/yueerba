package com.yueerba.framework.cache.redisson.builder;

import com.yueerba.framework.cache.utils.NodeConverter;
import com.yueerba.framework.cache.utils.ReflectionUtil;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description:
 *
 * ClusterConfigBuilder 类负责构建 Redis 集群的配置。
 * 该类依赖 RedisProperties 来获取集群的相关属性，并创建相应的 Redisson Config 对象。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@Component
public class ClusterConfigBuilder {

    // Redis 的相关属性，通常来源于应用的配置文件
    @Resource
    private RedisProperties redisProperties;

    /**
     * 根据 RedisProperties 构建 Redis 集群的配置。
     *
     * @param properties 应用配置中的 Redis 属性
     * @return 针对 Redis 集群的 Redisson Config 对象
     */
    public Config build(RedisProperties properties) {
        // 创建一个新的 Redisson 配置对象
        Config config = new Config();

        // 获取集群节点信息
        List<String> nodesObject = getClusterNodes(properties);

        // 转换节点信息，确保它们有正确的前缀（例如：redis://）
        String[] nodes = NodeConverter.convert(nodesObject);

        // 设置集群配置
        config.useClusterServers()
                .addNodeAddress(nodes)
                .setConnectTimeout(getTimeout(properties))
                .setPassword(properties.getPassword());

        return config;
    }

    /**
     * 从 RedisProperties 中获取集群的节点信息。
     *
     * @param properties 应用配置中的 Redis 属性
     * @return 集群节点的列表
     */
    private List<String> getClusterNodes(RedisProperties properties) {
        // 使用反射从 RedisProperties 获取集群节点信息
        Object clusterObject = ReflectionUtil.invokeMethod(RedisProperties.class, "getCluster", properties);
        return (List<String>) ReflectionUtil.invokeMethod(clusterObject.getClass(), "getNodes", clusterObject);
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

