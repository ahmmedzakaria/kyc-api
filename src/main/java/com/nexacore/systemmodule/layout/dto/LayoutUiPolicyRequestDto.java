package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class LayoutUiPolicyRequestDto {
    private String clientCode;
    private String actionCode;
    private PrivilegeMatchMode matchMode;
    private Boolean active;
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
