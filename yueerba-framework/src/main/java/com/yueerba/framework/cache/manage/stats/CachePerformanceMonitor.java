package com.yueerba.framework.cache.manage.stats;

package com.sydata.framework.cache.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存性能监控。
 * 这个类负责定期记录缓存的性能指标，并将其记录到日志中。
 */
@Slf4j
@RequiredArgsConstructor
public class CachePerformanceMonitor {

    /**
     * 缓存统计对象，用于获取缓存的性能指标
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
     * 启动缓存性能监控
     */
    public void start() {
        executorService.scheduleAtFixedRate(() -> {
            double hitRate = cacheStats.getHitRate();
            long averageLoadPenalty = cacheStats.getAverageLoadPenalty();
            log.info("当前缓存命中率: {}%，平均加载延迟: {} 毫秒", hitRate * 100, averageLoadPenalty);
        }, 0, monitorInterval, TimeUnit.SECONDS);
    }

    /**
     * 停止缓存性能监控
     */
    public void stop() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("缓存性能监控任务终止时发生中断", e);
        }
    }
}

