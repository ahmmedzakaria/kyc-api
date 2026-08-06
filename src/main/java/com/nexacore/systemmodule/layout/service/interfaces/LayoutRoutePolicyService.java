package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.commonmodule.dto.RoutePrivilegePolicyDto;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;

import java.util.Collection;
import java.util.List;

public interface LayoutRoutePolicyService {
    List<RoutePrivilegePolicyDto> getEffectivePolicies(String clientCode);

    void synchronizePolicy(String clientCode,
                           String routeUrl,
                           PrivilegeMatchMode matchMode,
                           Collection<String> privilegeCodes,
                           Long actorId);
}
