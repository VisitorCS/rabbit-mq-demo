package com.cs.rabbitmq;

import com.cs.rabbitmq.client.RabbitMqClient;
import com.cs.rabbitmq.event.demo.DemoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("test")
public class DemoController {
    @Autowired
    private RabbitMqClient rabbitMqClient;

    @RequestMapping("demo")
    public String test(){
        DemoEvent demoEvent = new DemoEvent();
        demoEvent.setEventId(UUID.randomUUID());
        demoEvent.setMessage("hello world");
        demoEvent.setWhen(Instant.now());
        rabbitMqClient.sendDelayEventMessage(demoEvent,1000L);
        return "成功";
    }
}
