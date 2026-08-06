package com.nexacore.kycmodule.person.dto;

import com.nexacore.kycmodule.person.entity.PersonDocumentType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDocumentDto {
    private Long id;
    private Long personId;
    private Long profileId;
    private PersonDocumentType documentType;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
