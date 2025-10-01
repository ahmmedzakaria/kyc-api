package com.example.kyc.service;

import com.example.kyc.dto.KycDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface KycService {
    KycDto create(KycDto dto, MultipartFile photo) throws Exception;
    KycDto update( KycDto dto, MultipartFile photo) throws Exception;
    void delete(Long id) throws Exception;
    KycDto getById(Long id);
    Page<KycDto> search(String q, String nationalId, int page, int size, String sort) ;
    byte[] getPhoto(Long id) throws Exception;
    String getPhotoContentType(Long id);
}
