package com.nexacore.servicesmodule.queueingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessageRequest {

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String messageId;

    private String correlationId;

    private Object payload;

    @Builder.Default
    private Map<String, Object> headers = new HashMap<>();

    @Builder.Default
    private boolean durable = true;
}
