package com.cs.rabbitmq.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class DelayEventProperties {
    @Value("${spring.rabbitmq.delay.exchange}")
    private String exchange;
    @Value("${spring.rabbitmq.delay.queue}")
    private String queue;
    @Value("${spring.rabbitmq.delay.routingKey}")
    private String routingKey;
}
