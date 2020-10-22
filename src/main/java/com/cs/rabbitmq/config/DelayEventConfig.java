package com.cs.rabbitmq.config;

import com.cs.rabbitmq.event.EventHandlerFactory;
import com.cs.rabbitmq.event.demo.DemoEventHandler;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayEventConfig {
    @Autowired
    private DelayEventProperties delayEventProperties;

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>(1);
        args.put("x-delayed-type", "direct");
        return new CustomExchange(delayEventProperties.getExchange(), "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue queue() {
        return new Queue(delayEventProperties.getQueue(), true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(delayExchange()).with(delayEventProperties.getRoutingKey()).noargs();
    }

    @Bean
    public EventHandlerFactory eventHandlerFactory() {
        return new EventHandlerFactory();
    }

    @Bean
    public DemoEventHandler demoEventHandle(EventHandlerFactory eventHandlerFactory) {
        DemoEventHandler demoEventHandler = new DemoEventHandler();
        eventHandlerFactory.register(demoEventHandler);
        return demoEventHandler;
    }
}