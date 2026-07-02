package com.nexacore.servicesmodule.messagingservice.dto;

import com.nexacore.servicesmodule.messagingservice.enums.MobileMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMessageRequest {

    private String mobileNumber;

    private String message;

    @Builder.Default
    private MobileMessageType messageType = MobileMessageType.GENERAL;

    private String referenceId;

    private Map<String, Object> metadata;
}

