package com.cs.rabbitmq.event.demo;

import com.cs.rabbitmq.event.BasicEvent;
import lombok.Data;

@Data
public class DemoEvent extends BasicEvent {
    private String message;
}
