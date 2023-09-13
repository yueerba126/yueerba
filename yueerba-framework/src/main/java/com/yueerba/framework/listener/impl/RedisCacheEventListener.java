package com.yueerba.framework.listener.impl;

import com.yueerba.framework.listener.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:
 *
 * RedisCacheEventListener 监听 Redis 的键空间事件。
 * 当 Redis 中的缓存条目发生特定的事件（例如条目过期、条目被删除等）时，该监听器将被触发。
 * 这个类主要是为了记录和处理这些事件。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
public class RedisCacheEventListener implements CacheEventListener {

    // 使用 SLF4J 进行日志记录
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheEventListener.class);

    @Override
    public void onPut(Object key, Object value) {
        logger.info("缓存条目被添加，键：{}，值：{}", key, value);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onEvict(Object key) {
        logger.info("缓存条目被移除，键：{}", key);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onExpire(Object key) {
        logger.info("缓存条目已过期，键：{}", key);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onRenameFrom(Object oldKey) {
        logger.info("缓存条目的键被重命名（旧键名）：{}", oldKey);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onRenameTo(Object newKey, Object newValue) {
        logger.info("缓存条目的键被重命名（新键名）：{}，新值：{}", newKey, newValue);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onClear() {
        logger.info("所有缓存条目被清空");
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }

    @Override
    public void onError(Exception exception) {
        logger.error("处理缓存事件时发生错误", exception);
        // 可以添加更多的处理逻辑，例如统计、通知等。
    }
}

