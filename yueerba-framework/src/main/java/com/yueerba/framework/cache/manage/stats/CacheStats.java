package com.yueerba.framework.cache.manage.stats;

package com.sydata.framework.cache.stats;

import lombok.Data;

/**
 * 缓存统计。
 * 提供了缓存的各种统计数据，例如命中率、缓存大小等。
 */
@Data
public class CacheStats {

    /**
     * 总的请求次数
     */
    private long requestCount;

    /**
     * 命中的次数
     */
    private long hitCount;

    /**
     * 未命中的次数
     */
    private long missCount;

    /**
     * 缓存大小
     */
    private long cacheSize;

    /**
     * 缓存的最大大小
     */
    private long maxCacheSize;

    /**
     * 获取缓存的命中率。
     *
     * @return 命中率
     */
    public double getHitRate() {
        return requestCount == 0 ? 0 : (double) hitCount / requestCount;
    }

    /**
     * 获取缓存的未命中率。
     *
     * @return 未命中率
     */
    public double getMissRate() {
        return requestCount == 0 ? 0 : (double) missCount / requestCount;
    }

    /**
     * 当请求命中缓存时，更新相关统计。
     */
    public void recordHits(int count) {
        requestCount += count;
        hitCount += count;
    }

    /**
     * 当请求未命中缓存时，更新相关统计。
     */
    public void recordMisses(int count) {
        requestCount += count;
        missCount += count;
    }

    /**
     * 更新缓存的大小。
     *
     * @param size 当前的缓存大小
     */
    public void updateCacheSize(long size) {
        this.cacheSize = size;
    }

    /**
     * 设置缓存的最大大小。
     *
     * @param maxCacheSize 缓存的最大大小
     */
    public void setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
}

