package com.yueerba.framework.cache.manage.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CacheChange 类表示缓存中的变化事件。
 * 这个类包含了事件类型（如添加、更新或删除）、键和值等属性。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheChange {

    /**
     * 缓存事件的类型。
     */
    public enum EventType {
        LOOK,       // 检索事件
        ADD,       // 添加事件
        UPDATE,    // 更新事件
        DELETE     // 删除事件
    }

    /**
     * 事件类型。
     */
    private EventType eventType;

    /**
     * 缓存的键。
     */
    private String key;

    /**
     * 缓存的值。对于删除事件，这个值可能为null。
     */
    private Object value;

    /**
     * 创建一个新的添加或更新事件。
     *
     * @param key   缓存的键
     * @param value 缓存的值
     * @return 一个新的 CacheChange 对象
     */
    public static CacheChange createAddOrUpdateEvent(String key, Object value) {
        return new CacheChange(EventType.ADD, key, value);
    }

    /**
     * 创建一个新的删除事件。
     *
     * @param key 缓存的键
     * @return 一个新的 CacheChange 对象
     */
    public static CacheChange createDeleteEvent(String key) {
        return new CacheChange(EventType.DELETE, key, null);
    }
}
