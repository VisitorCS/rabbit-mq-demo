package com.cs.rabbitmq.event;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理工厂
 */
@Slf4j
public class EventHandlerFactory<T extends EventHandler> {
    protected Map<String, T> registerMap = new ConcurrentHashMap<>();

    /**
     * 事件注册
     */
    public void register(T eventHandle) {
        if (registerMap.get(eventHandle.supportEventType()) != null) {
            log.warn("");
        }
        registerMap.put(eventHandle.supportEventType(), eventHandle);
    }

    /**
     * 事件处理选择器
     */
    public EventHandler getEventHandler(String key) {
        return registerMap.get(key);
    }

}
