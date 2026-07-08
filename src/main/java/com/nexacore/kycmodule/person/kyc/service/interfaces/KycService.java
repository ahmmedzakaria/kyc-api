package com.nexacore.kycmodule.person.kyc.service.interfaces;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.kycmodule.person.kyc.dto.KycDto;
import com.nexacore.commonmodule.dto.SearchDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface KycService {
    ResponseEntity<ApiResponse<KycDto>> create(KycDto dto, MultipartFile photo) throws Exception;
    ResponseEntity<ApiResponse<KycDto>> update( KycDto dto, MultipartFile photo) throws Exception;
    ResponseEntity<ApiResponse<Void>>  delete(Long id) throws Exception;
    ResponseEntity<ApiResponse<KycDto>>  getById(Long id);
    ResponseEntity<ApiResponse<Page<KycDto>>> search(SearchDto searchDto) ;
    byte[] getPhoto(Long id) throws Exception;
    String getPhotoContentType(Long id);
}
