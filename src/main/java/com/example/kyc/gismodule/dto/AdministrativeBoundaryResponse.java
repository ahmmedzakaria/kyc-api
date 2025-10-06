package com.example.kyc.gismodule.dto;

import java.util.UUID;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministrativeBoundaryResponse {
    private UUID id;
    private String villageName;
    private String unionName;
    private String subDistrictName;
    private String districtName;
    private String divisionName;
}

