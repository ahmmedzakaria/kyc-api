package com.nexacore.servicesmodule.emailservice.dto;

import com.nexacore.servicesmodule.emailservice.enums.EmailMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageRequest {

    private List<String> to;

    private List<String> cc;

    private List<String> bcc;

    private String subject;

    private String body;

    @Builder.Default
    private boolean html = false;

    @Builder.Default
    private EmailMessageType messageType = EmailMessageType.GENERAL;

    private String referenceId;

    private Map<String, Object> metadata;
}

