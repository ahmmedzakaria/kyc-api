package com.nexacore.servicesmodule.fileservice.service.implementations;

import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import com.nexacore.servicesmodule.fileservice.storage.interfaces.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileManagementServiceImpl implements FileManagementService {

    private final StorageProvider storageProvider;

    @Override
    public StoredFile store(String moduleName, String fileGroup, MultipartFile file, String existingPath) throws IOException {
        String path = storageProvider.store(buildPrefix(moduleName, fileGroup), file, existingPath);
        return new StoredFile(
                path,
                storageProvider.publicUrl(path),
                file.getContentType(),
                file.getOriginalFilename(),
                file.getSize()
        );
    }

    @Override
    public byte[] read(String pathOrUrl) throws IOException {
        return storageProvider.read(pathOrUrl);
    }

    @Override
    public boolean delete(String pathOrUrl) throws IOException {
        return storageProvider.delete(pathOrUrl);
    }

    @Override
    public String contentType(String pathOrUrl) {
        return storageProvider.contentType(pathOrUrl);
    }

    @Override
    public String publicUrl(String pathOrUrl) {
        String normalized = storageProvider.normalizePath(pathOrUrl);
        return normalized == null ? null : storageProvider.publicUrl(normalized);
    }

    private String buildPrefix(String moduleName, String fileGroup) {
        return sanitize(moduleName) + "/" + sanitize(fileGroup);
    }

    private String sanitize(String value) {
        String safe = Objects.requireNonNullElse(value, "common")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/_-]+", "-")
                .replaceAll("/+", "/")
                .replaceAll("^-+|-+$", "");
        return safe.isBlank() ? "common" : safe;
    }
}
