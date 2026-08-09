package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import lombok.Builder;

@Builder
public record ClientAccessDecisionDto(
        boolean allowed,
        String denyReason,
        SysAccClientApplication clientApplication,
        SysAccApiRegistry apiRegistry
) {
    public static ClientAccessDecisionDto allowed(SysAccClientApplication clientApplication, SysAccApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(true)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }

    public static ClientAccessDecisionDto denied(String denyReason,
                                                 SysAccClientApplication clientApplication,
                                                 SysAccApiRegistry apiRegistry) {
        return ClientAccessDecisionDto.builder()
                .allowed(false)
                .denyReason(denyReason)
                .clientApplication(clientApplication)
                .apiRegistry(apiRegistry)
                .build();
    }
}
