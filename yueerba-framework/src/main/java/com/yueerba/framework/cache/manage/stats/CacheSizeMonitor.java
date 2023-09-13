package com.yueerba.framework.cache.manage.stats;

package com.sydata.framework.cache.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存大小监控。
 * 这个类负责定期记录缓存的大小，并将其记录到日志中。
 */
@Slf4j
@RequiredArgsConstructor
public class CacheSizeMonitor {

    /**
     * 缓存统计对象，用于获取缓存的大小等统计信息
     */
    private final CacheStats cacheStats;

    /**
     * 监控的间隔时间，单位为秒
     */
    private final int monitorInterval;

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * 启动缓存大小监控
     */
    public void start() {
        executorService.scheduleAtFixedRate(() -> {
            long currentSize = cacheStats.getSize();
            log.info("当前缓存大小: {} 条记录", currentSize);
        }, 0, monitorInterval, TimeUnit.SECONDS);
    }

    /**
     * 停止缓存大小监控
     */
    public void stop() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("缓存大小监控任务终止时发生中断", e);
        }
    }
}

