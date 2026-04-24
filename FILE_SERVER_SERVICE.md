# File Server Service

This backend now supports a reusable file service abstraction for module-scoped uploads with either:

- `filesystem` storage for local/dev usage
- `minio` storage for object storage usage

## Key pieces

- `FileManagementService`: service contract consumed by feature modules
- `FileManagementServiceImpl`: reusable module-aware service implementation
- `StorageProvider`: low-level storage contract
- `FilesystemStorageProvider`: local disk implementation
- `MinioStorageProvider`: MinIO implementation

## Example usage

```java
StoredFile storedFile = fileManagementService.store("person", "profile-photo", multipartFile, existingPath);
String storagePath = storedFile.path();
String publicUrl = storedFile.publicUrl();
```

Use `storagePath` when you want an internal key for later reads/deletes.
Use `publicUrl` when you want a browser-accessible URL.

## MinIO setup

Run:

```bash
backend/scripts/install-minio.sh
```

Then:

```bash
backend/tools/minio/bin/start-minio.sh
backend/tools/minio/bin/create-bucket.sh
```

## Backend env

```bash
export STORAGE_TYPE=minio
export MINIO_ENDPOINT=http://127.0.0.1:9000
export MINIO_PUBLIC_BASE_URL=http://127.0.0.1:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=kyc-files
```
