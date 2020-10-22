package com.cs.rabbitmq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:rabbit-declare.properties")
public class RabbitDeclareConfig {
}
