package com.cs.rabbitmq.event;

import com.alibaba.fastjson.JSON;
import com.cs.rabbitmq.client.RabbitMqClient;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 延时队列监听器
 * @author cs
 */
@Component
@Slf4j
public class DelayEventListener {
    @Autowired
    private EventHandlerFactory eventHandlerFactory;

    @RabbitListener(queues = "${spring.rabbitmq.delay.queue}")
    public void rabbitListener(Channel channel, Message message) throws IOException {
        String messageBody = new String(message.getBody());
        log.info("receive event message: {}", messageBody);
        String eventType = (String) message.getMessageProperties().getHeaders().get(BasicEvent.TYPE);
        EventHandler eventHandler = eventHandlerFactory.getEventHandler(eventType);
        if (eventHandler != null) {
            try {
                eventHandler.handle(JSON.parseObject(messageBody, eventHandler.getEventType()));
            } catch (Exception e) {
                log.error("handle error. {},type:{}", messageBody, eventType, e);
            }
        } else {
            log.warn("Unsupported event type: {}", eventType);
        }
        RabbitMqClient.basicAck(channel, message);
    }
}
