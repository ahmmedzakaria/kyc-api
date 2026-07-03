package com.nexacore.servicesmodule.queueingservice.dto;

import com.nexacore.servicesmodule.queueingservice.enums.QueueMessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessageResponse {

    private boolean success;

    private QueueMessageStatus status;

    private String providerName;

    private String messageId;

    private String correlationId;

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String errorMessage;

    private Instant publishedAt;
}
