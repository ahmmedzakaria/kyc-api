package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientApplicationContextDto {
    private Long clientApplicationId;
    private String clientCode;
    private ClientApplicationType clientType;
}
