package com.cs.rabbitmq.builder;

import java.io.Serializable;
import java.util.Map;

/**
 * @author cs
 */
public abstract class BaseMqMessage implements Serializable {

    public abstract Map<String,Object> getMapResult();
}
