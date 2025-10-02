package com.example.kyc.commonmodule.service.implementations;

import com.example.kyc.commonmodule.service.interfaces.StorageService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/*
 NOTE: This is a stub implementation and NOT functional.
 If you want S3 storage:
  - add AWS SDK dependency (aws-java-sdk-s3 or AWS SDK v2)
  - implement actual upload/download/delete to S3 bucket
  - wire configuration properties (access key, secret, region, bucket)
*/
//@Service
public class S3StorageService implements StorageService {
    @Override
    public String store(MultipartFile file, String existingPath) throws IOException {
        throw new UnsupportedOperationException("S3 storage not implemented. Use filesystem or implement S3StorageService.");
    }

    @Override
    public byte[] read(String path) throws IOException {
        throw new UnsupportedOperationException("S3 storage not implemented.");
    }

    @Override
    public String contentType(String path) {
        return null;
    }

    @Override
    public boolean delete(String path) throws IOException {
        throw new UnsupportedOperationException("S3 storage not implemented.");
    }

    @Override
    public Path resolveStoragePath(String path) {
        return null;
    }
}
