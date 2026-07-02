package com.nexacore.servicesmodule.messagingservice.dto;

import com.nexacore.servicesmodule.messagingservice.enums.MobileMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMessageResponse {

    private boolean success;

    private boolean skipped;

    private String providerName;

    private String mobileNumber;

    private MobileMessageType messageType;

    private String referenceId;

    private int statusCode;

    private String providerResponse;

    private String errorMessage;
}

