package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import lombok.Builder;

@Builder
public record ClientAccessDecisionDto(
        boolean allowed,
        String denyReason,
        SysPrivClientApplication clientApplication,
        SysPrivApiRegistry apiRegistry
) {
    public static ClientAccessDecisionDto allowed(SysPrivClientApplication clientApplication, SysPrivApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(true)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }

    public static ClientAccessDecisionDto denied(String denyReason,
                                                 SysPrivClientApplication clientApplication,
                                                 SysPrivApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(false)
                .denyReason(denyReason)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }
}
