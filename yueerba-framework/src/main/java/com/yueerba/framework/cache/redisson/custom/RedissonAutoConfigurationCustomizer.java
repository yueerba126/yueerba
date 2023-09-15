package com.yueerba.framework.cache.redisson.custom;

import org.redisson.config.Config;

/**
 * Description: redisson配置增强接口
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
@FunctionalInterface
public interface RedissonAutoConfigurationCustomizer {

    /**
     * 配置增强
     * @param configuration
     */
    void customize(final Config configuration);
}
