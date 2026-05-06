package com.nexacore.servicesmodule.fileservice.service.interfaces;

import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileManagementService {
    StoredFile store(String moduleName, String fileGroup, MultipartFile file, String existingPath) throws IOException;
    byte[] read(String pathOrUrl) throws IOException;
    boolean delete(String pathOrUrl) throws IOException;
    String contentType(String pathOrUrl);
    String publicUrl(String pathOrUrl);
}
