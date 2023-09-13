package com.yueerba.framework.cache.manage.support;

import org.springframework.cache.Cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Description: 多级缓存顶级接口
 *
 * 定义了用于多级缓存的核心操作，包括批量获取、设置和移除缓存项等功能。
 * 这个接口继承了Spring的Cache接口，因此也提供了基本的缓存操作。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
public interface MultiCache extends Cache {

    /**
     * 批量查询缓存。
     *
     * @param cacheKeys 缓存的keys列表
     * @param <V> 返回的缓存值的类型
     * @return 返回一个映射，其中键是缓存键，值是对应的缓存值。
     */
    <V> Map<String, V> batchGet(List<String> cacheKeys);

    /**
     * 批量设置缓存。
     *
     * @param map 批量缓存的映射，其中键是缓存键，值是缓存值。
     * @param <V> 缓存值的类型
     */
    <V> void batchPut(Map<String, V> map);

    /**
     * 批量移除缓存。
     *
     * @param cacheKeys 要移除的缓存的keys列表
     */
    void batchEvict(Collection<String> cacheKeys);

    /**
     * 转换成缓存Key。
     *
     * 将传入的原始key转换为缓存使用的key。这通常涉及添加前缀或其他格式化。
     *
     * @param key 原始key
     * @return 转换后的缓存key
     */
    String cacheKey(Object key);
}

