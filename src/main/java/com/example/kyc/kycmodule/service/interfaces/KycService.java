package com.example.kyc.kycmodule.service.interfaces;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.kycmodule.dto.KycDto;
import com.example.kyc.kycmodule.dto.KycSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface KycService {
    ResponseEntity<ApiResponse<KycDto>> create(KycDto dto, MultipartFile photo) throws Exception;
    ResponseEntity<ApiResponse<KycDto>> update( KycDto dto, MultipartFile photo) throws Exception;
    ResponseEntity<ApiResponse<Void>>  delete(Long id) throws Exception;
    ResponseEntity<ApiResponse<KycDto>>  getById(Long id);
    ResponseEntity<ApiResponse<Page<KycDto>>> search(KycSearchDto kycSearchDto) ;
    byte[] getPhoto(Long id) throws Exception;
    String getPhotoContentType(Long id);
}
