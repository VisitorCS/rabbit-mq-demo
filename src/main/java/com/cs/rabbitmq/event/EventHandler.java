package com.cs.rabbitmq.event;

import cn.hutool.core.util.TypeUtil;

/**
 * @author cs
 */
public interface EventHandler<T> {
    /**
     * 支持事件类型
     *
     * @return
     */
    String supportEventType();

    /**
     * 事件处理
     *
     * @param t
     */
    void handle(T t);

    /**
     * 获取事件对象
     * @implNote  this.getClass().getGenericInterfaces()[0] 获取当前类实现的接口类
     */
    default Class<T> getEventType() {
        return (Class<T>) TypeUtil.toParameterizedType(this.getClass().getGenericInterfaces()[0])
                        .getActualTypeArguments()[0];
    }
}
