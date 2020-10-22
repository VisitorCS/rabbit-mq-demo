package com.cs.rabbitmq.event.demo;

import com.alibaba.fastjson.JSON;
import com.cs.rabbitmq.event.EventHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DemoEventHandler implements EventHandler<DemoEvent> {
    @Override
    public String supportEventType() {
        return DemoEvent.class.getName();
    }

    @Override
    public void handle(DemoEvent demoEvent) {
        log.info("handle userFollowDelayEvent {}", JSON.toJSONString(demoEvent));
        System.out.println(JSON.toJSONString(demoEvent));
    }
}
