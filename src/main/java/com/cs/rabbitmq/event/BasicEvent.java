package com.cs.rabbitmq.event;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * @author cs
 */
@Data
public class BasicEvent {
    public static final String TYPE = "x-cs-type";
    /**
     * 领域事件ID
     */
    private UUID eventId;
    /**
     * 事件产生时间
     */
    private Instant when;
}
