package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.commonmodule.dto.UiPrivilegePolicyDto;
import com.nexacore.systemmodule.layout.dto.LayoutUiPolicyRequestDto;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;

import java.util.Collection;
import java.util.List;

public interface LayoutUiPolicyService {
    List<UiPrivilegePolicyDto> getEffectivePolicies(String clientCode);

    UiPrivilegePolicyDto save(LayoutUiPolicyRequestDto request, String actor);

    void synchronizePolicy(String clientCode,
                           String actionCode,
                           PrivilegeMatchMode matchMode,
                           Collection<String> privilegeCodes,
                           Long actorId);
}
