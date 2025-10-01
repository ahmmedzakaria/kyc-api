package com.example.kyc.service.impl;

import com.example.kyc.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilesystemStorageService implements StorageService {

    @Value("${storage.filesystem.base-dir:./data/kyc-photos}")
    private String baseDir;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
        log.info("Filesystem storage base dir={}", basePath);
    }

    @Override
    public String store(MultipartFile file, String existingPath) throws IOException {
        if (file == null || file.isEmpty()) return existingPath;
        // delete existing if present
        if (existingPath != null && !existingPath.isBlank()) {
            try {
                Files.deleteIfExists(basePath.resolve(existingPath));
            } catch (Exception e) {
                log.warn("failed to delete existing file {}: {}", existingPath, e.getMessage());
            }
        }
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID().toString() + ext;
        Path target = basePath.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    @Override
    public byte[] read(String path) throws IOException {
        if (path == null) return null;
        Path p = basePath.resolve(path);
        if (!Files.exists(p)) return null;
        return Files.readAllBytes(p);
    }

    @Override
    public String contentType(String path) {
        try {
            Path p = basePath.resolve(path);
            return Files.probeContentType(p);
        } catch (Exception e) {
            log.warn("probeContentType failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean delete(String path) throws IOException {
        if (path == null) return false;
        return Files.deleteIfExists(basePath.resolve(path));
    }

    @Override
    public java.nio.file.Path resolveStoragePath(String path) {
        return basePath.resolve(path);
    }
}
