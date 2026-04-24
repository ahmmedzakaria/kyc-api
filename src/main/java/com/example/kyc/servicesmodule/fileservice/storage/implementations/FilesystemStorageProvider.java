package com.example.kyc.servicesmodule.fileservice.storage.implementations;

import com.example.kyc.servicesmodule.fileservice.storage.interfaces.StorageProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "filesystem", matchIfMissing = true)
public class FilesystemStorageProvider implements StorageProvider {

    @Value("${storage.filesystem.base-dir:./data/kyc-photos}")
    private String baseDir;

    @Value("${storage.filesystem.public-base-url:/uploads}")
    private String publicBaseUrl;

    private Path basePath;

    @PostConstruct
    public void init() {
        try {
            basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
            log.info("Filesystem storage base dir initialized at {}", basePath);
        } catch (IOException e) {
            log.error("Failed to initialize storage directory: {}", baseDir, e);
            throw new IllegalStateException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(String keyPrefix, MultipartFile file, String existingPath) throws IOException {
        if (file == null || file.isEmpty()) return existingPath;
        String normalizedExistingPath = normalizePath(existingPath);
        if (normalizedExistingPath != null && !normalizedExistingPath.isBlank()) {
            try {
                Files.deleteIfExists(basePath.resolve(normalizedExistingPath));
            } catch (Exception e) {
                log.warn("failed to delete existing file {}: {}", normalizedExistingPath, e.getMessage());
            }
        }
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID() + ext;
        String relativePath = joinPath(keyPrefix, filename);
        Path target = basePath.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }

    @Override
    public byte[] read(String path) throws IOException {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return null;
        Path p = basePath.resolve(normalizedPath);
        if (!Files.exists(p)) return null;
        return Files.readAllBytes(p);
    }

    @Override
    public String contentType(String path) {
        try {
            String normalizedPath = normalizePath(path);
            if (normalizedPath == null) return null;
            Path p = basePath.resolve(normalizedPath);
            return Files.probeContentType(p);
        } catch (Exception e) {
            log.warn("probeContentType failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean delete(String path) throws IOException {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return false;
        return Files.deleteIfExists(basePath.resolve(normalizedPath));
    }

    @Override
    public Path resolveStoragePath(String path) {
        String normalizedPath = normalizePath(path);
        return normalizedPath == null ? null : basePath.resolve(normalizedPath);
    }

    @Override
    public String publicUrl(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return null;
        return trimTrailingSlash(publicBaseUrl) + "/" + normalizedPath;
    }

    @Override
    public String normalizePath(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return null;
        String normalized = pathOrUrl.trim();
        String publicPrefix = trimTrailingSlash(publicBaseUrl) + "/";
        if (normalized.startsWith(publicPrefix)) {
            normalized = normalized.substring(publicPrefix.length());
        }
        normalized = normalized.replaceFirst("^/+", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String joinPath(String keyPrefix, String filename) {
        String safePrefix = normalizePath(keyPrefix);
        return (safePrefix == null || safePrefix.isBlank()) ? filename : safePrefix + "/" + filename;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
