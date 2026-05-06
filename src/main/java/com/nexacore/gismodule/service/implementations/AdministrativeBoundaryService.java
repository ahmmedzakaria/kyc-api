package com.nexacore.gismodule.service.implementations;



import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.gismodule.dto.AdministrativeBoundaryResponse;
import com.nexacore.gismodule.entity.AdministrativeBoundariesLevel5;
import com.nexacore.gismodule.entity.AdministrativeBoundariesLevel6;
import com.nexacore.gismodule.enums.GisEntity;
import com.nexacore.gismodule.repository.AdministrativeBoundariesLevel5Repository;
import com.nexacore.gismodule.repository.AdministrativeBoundariesLevel6Repository;
import com.nexacore.commonmodule.dto.SearchDto;
import com.nexacore.kycmodule.entity.KycRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdministrativeBoundaryService {

    private final AdministrativeBoundariesLevel5Repository administrativeBoundariesLevel5Repository;
    private final AdministrativeBoundariesLevel6Repository administrativeBoundariesLevel6Repository;

    public ApiResponse<Page<AdministrativeBoundaryResponse>> search(SearchDto dto) {
      try{
        Page<KycRecord> pageData = null;
        Pageable pageable = PageRequest.of(dto.page(), dto.size());
        Page<AdministrativeBoundariesLevel5> page = administrativeBoundariesLevel5Repository.searchByAnyLevel(dto.searchText(), pageable);

        Page<AdministrativeBoundaryResponse> response =  page.map(a -> new AdministrativeBoundaryResponse(
                a.getId(),
                GisEntity.ADMINISTRATIVE_BOUNDARY_LEVEL_5.getGisCode(),
                a.getUnionName(),
                a.getSubDistrictName(),
                a.getDistrictName(),
                a.getDivisionName(),
                a.getUnionName()
                        +" / " +a.getSubDistrictName()+" / "+a.getDivisionName()
        ));
          return ApiResponse.success(response, "Search Completed Successfully");
        //return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, "Search Completed Successfully"));
    } catch (Exception e) {
          return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage()));

//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
    }
    }

    public ApiResponse<AdministrativeBoundaryResponse> getById(UUID id, String gisCode) {
        try {
            GisEntity gisEntity = GisEntity.fromCode(gisCode);
            AdministrativeBoundaryResponse response = switch (gisEntity) {
                case ADMINISTRATIVE_BOUNDARY_LEVEL_5 -> administrativeBoundariesLevel5Repository.findById(id)
                        .map(this::toLevel5Response)
                        .orElseThrow(() -> new IllegalArgumentException("Location not found"));
                case ADMINISTRATIVE_BOUNDARY_LEVEL_6 -> administrativeBoundariesLevel6Repository.findById(id)
                        .map(this::toLevel6Response)
                        .orElseThrow(() -> new IllegalArgumentException("Location not found"));
                default -> throw new IllegalArgumentException("Lookup not implemented for GIS code: " + gisCode);
            };

            return ApiResponse.success(response, "Location fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage()));
        }
    }

    private AdministrativeBoundaryResponse toLevel5Response(AdministrativeBoundariesLevel5 a) {
        return new AdministrativeBoundaryResponse(
                a.getId(),
                GisEntity.ADMINISTRATIVE_BOUNDARY_LEVEL_5.getGisCode(),
                a.getUnionName(),
                a.getSubDistrictName(),
                a.getDistrictName(),
                a.getDivisionName(),
                a.getUnionName() + " / " + a.getSubDistrictName() + " / " + a.getDivisionName()
        );
    }

    private AdministrativeBoundaryResponse toLevel6Response(AdministrativeBoundariesLevel6 a) {
        return new AdministrativeBoundaryResponse(
                a.getId(),
                GisEntity.ADMINISTRATIVE_BOUNDARY_LEVEL_6.getGisCode(),
                a.getUnionName(),
                a.getSubDistrictName(),
                a.getDistrictName(),
                a.getDivisionName(),
                a.getVillageName() + " / " + a.getUnionName() + " / " + a.getSubDistrictName() + " / " + a.getDivisionName()
        );
    }
}
