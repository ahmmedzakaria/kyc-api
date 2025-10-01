package com.example.kyc.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {
    // stores file and returns storage path (relative or absolute depending on impl)
    String store(MultipartFile file, String existingPath) throws IOException;
    byte[] read(String path) throws IOException;
    String contentType(String path);
    boolean delete(String path) throws IOException;
    Path resolveStoragePath(String path);
}
