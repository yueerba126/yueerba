package com.yueerba.framework.listener;

/**
 * Description:
 *
 * CacheEventListener 定义了一个缓存事件的监听器。
 * 当缓存发生特定的事件（例如条目过期、条目被删除等）时，该接口的实现将被通知。
 * 这使得我们可以在事件发生时执行特定的操作，如日志记录、更新统计信息或触发其他业务逻辑。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
public interface CacheEventListener {

    /**
     * 当缓存条目被创建或更新时触发。
     *
     * @param key 被创建或更新的条目的键。
     * @param value 被创建或更新的条目的值。
     */
    void onPut(Object key, Object value);

    /**
     * 当缓存条目被删除时触发。
     *
     * @param key 被删除的条目的键。
     */
    void onEvict(Object key);

    /**
     * 当缓存条目过期时触发。
     *
     * @param key 过期条目的键。
     */
    void onExpire(Object key);

    /**
     * 当缓存被清除（例如，所有条目都被删除）时触发。
     */
    void onClear();

    /**
     * 当缓存条目被重命名（旧键）时触发。
     *
     * @param oldKey 被重命名的条目的旧键。
     */
    void onRenameFrom(Object oldKey);

    /**
     * 当缓存条目被重命名（新键）时触发。
     *
     * @param newKey 被重命名的条目的新键。
     * @param value 被重命名的条目的值。
     */
    void onRenameTo(Object newKey, Object value);

    /**
     * 当缓存出现任何异常情况时触发。
     *
     * @param exception 出现的异常。
     */
    void onError(Exception exception);
}


