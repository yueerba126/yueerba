package com.yueerba.framework.cache.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 *
 * NodeConverter 类负责转换 Redis 节点的地址格式。
 * 该类确保每个节点的地址都有正确的前缀（例如：redis://）。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
public class NodeConverter {

    // Redis 默认的协议前缀
    public static final String REDIS_PROTOCOL_PREFIX = "redis://";
    public static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    /**
     * 转换给定的节点地址列表，确保每个地址都有正确的前缀。
     *
     * @param nodesObject 原始的节点地址列表
     * @return 转换后的节点地址列表
     */
    public static String[] convert(List<String> nodesObject) {
        // 使用 ArrayList 来存储转换后的地址
        List<String> nodes = new ArrayList<>(nodesObject.size());

        // 遍历每个原始地址
        for (String node : nodesObject) {
            // 如果地址不包含期望的前缀，则添加前缀
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(REDIS_PROTOCOL_PREFIX + node);
            } else {
                nodes.add(node);
            }
        }

        // 将 List 转换为数组并返回
        return nodes.toArray(new String[nodes.size()]);
    }
}

