package com.cs.rabbitmq.client;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson.JSON;
import com.cs.rabbitmq.config.DelayEventProperties;
import com.cs.rabbitmq.event.BasicEvent;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import com.google.common.util.concurrent.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@Slf4j
public class RabbitMqClient {
    public static final String TIMESTAMP = "x-cs-timestamp";

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private DelayEventProperties delayEventProperties;

    /**
     * 异步发送消息
     *
     * @param exchange
     * @param routingKey
     * @param message
     */
    public void asySendMessage(String exchange, String routingKey, Object message) {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            String messageBody = JSON.toJSONString(message);
            // 使用guava提供的MoreExecutors工具类包装原始的线程池
            ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(pool);
            // 向线程池中提交一个任务后，将会返回一个可监听的Future，该Future由Guava框架提供
            ListenableFuture<Boolean> lf = listeningExecutor.submit(() -> {
                Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                        //抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。
                        .retryIfException()
                        //返回false也需要重试（可以根据返回值确定需不需要重试）
                        //.retryIfResult(Predicates.equalTo(false))
                        //重调策略
                        .withWaitStrategy(WaitStrategies.incrementingWait(15, TimeUnit.SECONDS, 15, TimeUnit.SECONDS))
                        //尝试次数
                        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                        .build();
                retryer.call(() -> sendMessage(exchange, routingKey, message));
                return true;
            });

            // 添加回调，回调由executor中的线程触发，但也可以指定一个新的线程
            Futures.addCallback(lf, new FutureCallback<Boolean>() {
                // 耗时任务执行失败后回调该方法
                @Override
                public void onFailure(Throwable t) {
                    log.error(String.format("sendMessage Error[exchange:%s message:%s error:%s]", exchange, messageBody, t));
                }

                @Override
                public void onSuccess(Boolean result) {
                    log.info(String.format("sendMessage success [exchange:%s message:%s]", exchange, messageBody));
                }
            }, pool);
        } catch (Exception e) {
            log.error(String.format("sendMessage Error[error:%s]", e));
        } finally {
            pool.shutdown();
        }
    }


    /**
     * 同步发送
     *
     * @param exchange
     * @param routingKey
     * @param message
     * @return
     */
    private Boolean sendMessage(String exchange, String routingKey, Object message) {
        try {
            String messageBody = JSON.toJSONString(message);
            String messageId = UUID.randomUUID().toString();
            Message builderMsg = MessageBuilder.withBody(messageBody.getBytes())
                    .setContentType(MessageProperties.TEXT_PLAIN.getContentType())
                    .setContentEncoding("utf-8")
                    .setMessageId(messageId).build();
            CorrelationData correlationData = new CorrelationData(messageId);

            log.info("send message body:{} id:{}", messageBody, messageId);
            rabbitTemplate.convertAndSend(exchange, routingKey, builderMsg, correlationData);
            return true;
        } catch (Exception e) {
            log.error("send message error:{}", ExceptionUtil.stacktraceToString(e));
        }
        return false;
    }

    /**
     * 发送延迟消息
     *
     * @param exchange
     * @param routingKey
     * @param message
     * @param millisecond 延迟毫秒数
     * @return
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, long millisecond) {
        return sendDelayMessage(exchange, routingKey, message, null, millisecond);
    }


    /**
     * 发送延迟事件消息
     *
     * @param event
     * @param millisecond
     * @return
     */
    public boolean sendDelayEventMessage(BasicEvent event, long millisecond) {
        return sendDelayEventMessage(delayEventProperties.getExchange(), delayEventProperties.getRoutingKey(), event, millisecond);
    }

    /**
     * 发送延迟事件消息
     *
     * @param exchange
     * @param routingKey
     * @param event
     * @param millisecond
     * @return
     */
    public boolean sendDelayEventMessage(String exchange, String routingKey, BasicEvent event, long millisecond) {
        Map<String, Object> header = new HashMap<>(8);
        header.put(BasicEvent.TYPE, event.getClass().getName());
        header.put(TIMESTAMP, event.getWhen());
        return sendDelayMessage(exchange, routingKey, event, header, millisecond);
    }

    /**
     * 发送延迟消息
     *
     * @param exchange
     * @param routingKey
     * @param message
     * @param header
     * @param millisecond 延迟毫秒数
     * @return
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, Map<String, Object> header, long millisecond) {
        try {
            String messageBody = JSON.toJSONString(message);
            String messageId = UUID.randomUUID().toString();

            Message builderMsg = MessageBuilder.withBody(messageBody.getBytes())
                    .setContentType(MessageProperties.TEXT_PLAIN.getContentType())
                    .setContentEncoding("utf-8")
                    .setHeader("x-delay", millisecond)
                    .setMessageId(messageId).build();
            if (header != null) {
                for (String key : header.keySet()) {
                    builderMsg.getMessageProperties().setHeader(key, header.get(key));
                }
            }
            CorrelationData correlationData = new CorrelationData(messageId);
            log.info("send message body:{} id:{}", messageBody, messageId);
            rabbitTemplate.convertAndSend(exchange, routingKey, builderMsg, correlationData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("send message error:{}", ExceptionUtil.stacktraceToString(e));
        }
        return false;
    }

    /**
     * 重新发送到队列中(队列尾)
     *
     * @throws IOException
     */
    public static void basicPublish(String messageBody, Channel channel, Message message) throws IOException {
        //手动进行应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        //重新发送消息到队尾
        channel.basicPublish(message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(), MessageProperties.PERSISTENT_TEXT_PLAIN, messageBody.getBytes());
    }

    /**
     * 重新发送到队列(队列头)
     *
     * @throws IOException
     */
    public static void basicNack(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }

    /**
     * 手工确认消息已读取
     *
     * @throws IOException
     */
    public static void basicAck(Channel channel, Message message) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
