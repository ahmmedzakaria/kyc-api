package com.nexacore.kycmodule.dto;

import com.nexacore.kycmodule.entity.PersonDocumentType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDocumentDto {
    private Long id;
    private Long personId;
    private PersonDocumentType documentType;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
