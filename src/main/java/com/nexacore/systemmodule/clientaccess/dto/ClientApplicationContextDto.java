package com.nexacore.systemmodule.clientaccess.dto;

import com.nexacore.systemmodule.clientaccess.enums.ClientApplicationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientApplicationContextDto {
    private Long clientApplicationId;
    private String clientCode;
    private ClientApplicationType clientType;
}
