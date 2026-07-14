package com.nexacore.systemmodule.privilege.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedClientCredentialDto {
    private String clientCode;
    private String clientId;
    private String apiKey;
}
