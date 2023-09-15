package com.yueerba.framework.cache.batch;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Description: 多级缓存顶级接口
 * Author: yueerba
 * Date: 2023/9/15
 */
public interface BatchOperationInterface {
    /**
     * 批量查询缓存
     *
     * @param cacheKeys 缓存的keys
     * @return 缓存的值
     */
    <V> Map<String, V> batchGet(List<String> cacheKeys);

    /**
     * 批量设置缓存
     *
     * @param map 批量缓存
     */
    <V> void batchPut(Map<String, V> map);

    /**
     * 批量移除缓存
     *
     * @param cacheKeys 缓存的keys
     */
    void batchEvict(Collection<String> cacheKeys);

    /**
     * 转换成缓存Key
     *
     * @param key 原始key
     * @return 缓存key
     */
    String cacheKey(Object key);
}
