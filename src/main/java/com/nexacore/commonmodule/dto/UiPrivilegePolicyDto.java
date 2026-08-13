package com.nexacore.commonmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UiPrivilegePolicyDto {
    private Long id;
    private Long version;
    private Long clientApplicationId;
    private String clientCode;
    private String actionCode;
    private String matchMode;
    private Boolean active;

    @Builder.Default
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
