package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientApplicationDto {
    private Long id;
    private String clientCode;
    private String clientName;
    private ClientApplicationType clientType;
    private ClientApplicationStatus status;
    private String allowedOrigins;
    private String allowedIps;
    private Integer rateLimitPerMinute;
    private String description;

    public static ClientApplicationDto fromEntity(SysAccClientApplication application) {
        return ClientApplicationDto.builder()
                .id(application.getId())
                .clientCode(application.getClientCode())
                .clientName(application.getClientName())
                .clientType(application.getClientType())
                .status(application.getStatus())
                .allowedOrigins(application.getAllowedOrigins())
                .allowedIps(application.getAllowedIps())
                .rateLimitPerMinute(application.getRateLimitPerMinute())
                .description(application.getDescription())
                .build();
    }
}
