package com.nexacore.systemmodule.clientaccess.dto;

import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
import lombok.Builder;

@Builder
public record ClientAccessDecisionDto(
        boolean allowed,
        String denyReason,
        SysClientApplication clientApplication,
        SysApiRegistry apiRegistry
) {
    public static ClientAccessDecisionDto allowed(SysClientApplication clientApplication, SysApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(true)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }

    public static ClientAccessDecisionDto denied(String denyReason,
                                                 SysClientApplication clientApplication,
                                                 SysApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(false)
                .denyReason(denyReason)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }
}
