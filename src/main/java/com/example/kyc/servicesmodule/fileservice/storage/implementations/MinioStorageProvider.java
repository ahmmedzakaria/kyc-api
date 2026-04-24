package com.example.kyc.servicesmodule.fileservice.storage.implementations;

import com.example.kyc.servicesmodule.fileservice.storage.interfaces.StorageProvider;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageProvider implements StorageProvider {

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.public-base-url:${storage.minio.endpoint}}")
    private String publicBaseUrl;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket}")
    private String bucket;

    @Value("${storage.minio.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        if (autoCreateBucket) {
            ensureBucket();
        }
    }

    @Override
    public String store(String keyPrefix, MultipartFile file, String existingPath) throws IOException {
        if (file == null || file.isEmpty()) return existingPath;
        String normalizedExistingPath = normalizePath(existingPath);
        if (normalizedExistingPath != null) {
            delete(normalizedExistingPath);
        }

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        String objectName = joinPath(keyPrefix, UUID.randomUUID() + ext);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new IOException("Failed to store file in MinIO", e);
        }
    }

    @Override
    public byte[] read(String path) throws IOException {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return null;
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(normalizedPath)
                        .build()
        )) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new IOException("Failed to read file from MinIO", e);
        }
    }

    @Override
    public String contentType(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return null;
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(normalizedPath)
                            .build()
            ).contentType();
        } catch (Exception e) {
            log.warn("Failed to fetch MinIO content type for {}: {}", normalizedPath, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean delete(String path) throws IOException {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return false;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(normalizedPath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            throw new IOException("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public Path resolveStoragePath(String path) {
        return null;
    }

    @Override
    public String publicUrl(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) return null;
        return trimTrailingSlash(publicBaseUrl) + "/" + bucket + "/" + normalizedPath;
    }

    @Override
    public String normalizePath(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return null;
        String normalized = pathOrUrl.trim();
        String publicPrefix = trimTrailingSlash(publicBaseUrl) + "/" + bucket + "/";
        if (normalized.startsWith(publicPrefix)) {
            normalized = normalized.substring(publicPrefix.length());
        }
        normalized = normalized.replaceFirst("^/+", "");
        return normalized.isBlank() ? null : normalized;
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket {}", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket " + bucket, e);
        }
    }

    private String joinPath(String keyPrefix, String filename) {
        String normalizedPrefix = normalizePath(keyPrefix);
        return (normalizedPrefix == null || normalizedPrefix.isBlank()) ? filename : normalizedPrefix + "/" + filename;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
