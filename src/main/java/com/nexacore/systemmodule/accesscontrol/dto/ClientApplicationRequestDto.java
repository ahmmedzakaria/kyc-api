package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import lombok.Data;

@Data
public class ClientApplicationRequestDto {
    private Long id;
    private String clientCode;
    private String clientName;
    private ClientApplicationType clientType;
    private ClientApplicationStatus status;
    private String allowedOrigins;
    private String allowedIps;
    private Integer rateLimitPerMinute;
    private String description;
}
