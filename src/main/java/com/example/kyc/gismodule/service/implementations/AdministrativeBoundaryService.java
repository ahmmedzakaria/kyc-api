package com.example.kyc.gismodule.service.implementations;



import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.gismodule.dto.AdministrativeBoundaryResponse;
import com.example.kyc.gismodule.entity.AdministrativeBoundariesLevel5;
import com.example.kyc.gismodule.entity.AdministrativeBoundariesLevel6;
import com.example.kyc.gismodule.repository.AdministrativeBoundariesLevel5Repository;
import com.example.kyc.commonmodule.dto.SearchDto;
import com.example.kyc.kycmodule.entity.KycRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdministrativeBoundaryService {

    private final AdministrativeBoundariesLevel5Repository repository;

    public ApiResponse<Page<AdministrativeBoundaryResponse>> search(SearchDto dto) {
      try{
        Page<KycRecord> pageData = null;
        Pageable pageable = PageRequest.of(dto.page(), dto.size());
        Page<AdministrativeBoundariesLevel5> page = repository.searchByAnyLevel(dto.searchText(), pageable);

        Page<AdministrativeBoundaryResponse> response =  page.map(a -> new AdministrativeBoundaryResponse(
                a.getId(),
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
}

