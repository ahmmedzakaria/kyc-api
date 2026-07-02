package com.nexacore.servicesmodule.emailservice.dto;

import com.nexacore.servicesmodule.emailservice.enums.EmailMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageResponse {

    private boolean success;

    private boolean skipped;

    private String providerName;

    private List<String> to;

    private EmailMessageType messageType;

    private String referenceId;

    private String providerResponse;

    private String errorMessage;
}

