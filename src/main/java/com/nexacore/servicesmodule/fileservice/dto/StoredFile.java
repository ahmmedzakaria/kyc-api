package com.nexacore.servicesmodule.fileservice.dto;

public record StoredFile(
        String path,
        String publicUrl,
        String contentType,
        String originalFilename,
        long size
) {
}
