package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class LayoutRoutePolicyRequestDto {
    /** Blank = global (no client-specific override) — mirrors LayoutUiPolicyRequestDto's own convention. */
    private String clientCode;
    private String routeUrl;
    private PrivilegeMatchMode matchMode;
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
