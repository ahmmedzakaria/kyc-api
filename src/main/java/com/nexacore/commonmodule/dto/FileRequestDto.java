package com.nexacore.commonmodule.dto;

import lombok.Data;

@Data
public class FileRequestDto {
    private Long ownerId;
    private Long fileId;
    private String fileType;
}
