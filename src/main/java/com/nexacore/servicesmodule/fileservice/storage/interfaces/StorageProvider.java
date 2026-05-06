package com.nexacore.servicesmodule.fileservice.storage.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageProvider {
    default String store(MultipartFile file, String existingPath) throws IOException {
        return store(null, file, existingPath);
    }

    String store(String keyPrefix, MultipartFile file, String existingPath) throws IOException;
    byte[] read(String path) throws IOException;
    String contentType(String path);
    boolean delete(String path) throws IOException;
    Path resolveStoragePath(String path);
    String publicUrl(String path);
    String normalizePath(String pathOrUrl);
}
