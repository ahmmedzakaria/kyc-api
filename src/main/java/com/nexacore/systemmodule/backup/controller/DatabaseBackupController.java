package com.nexacore.systemmodule.backup.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import com.nexacore.systemmodule.backup.dto.*;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import com.nexacore.systemmodule.backup.service.BackupJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/system/backup")
@RequiredArgsConstructor
public class DatabaseBackupController {
    private final BackupJobService service;

    @PostMapping("/run")
    @PrivilegeApi("11060100113")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).DATABASE_BACKUP_EXECUTE)")
    public ResponseEntity<ApiResponse<BackupJobDto>> run(Authentication authentication) {
        BackupJobDto job = service.enqueue(BackupTrigger.MANUAL, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.successCode(
                job, "system.backup.queued", "Database backup queued"));
    }

    @PostMapping("/list")
    @PrivilegeApi("11060100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).DATABASE_BACKUP_VIEW)")
    public ApiResponse<BackupPageDto> list(@RequestBody(required = false) BackupListRequestDto request) {
        return ApiResponse.successCode(service.list(request), "system.backup.listed", "Database backups listed");
    }

    @PostMapping("/detail")
    @PrivilegeApi("11060100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).DATABASE_BACKUP_VIEW)")
    public ApiResponse<BackupJobDto> detail(@Valid @RequestBody BackupIdRequestDto request) {
        return ApiResponse.successCode(service.detail(request.backupId()), "system.backup.detail", "Database backup detail loaded");
    }

    @PostMapping("/download")
    @PrivilegeApi("11060100104")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).DATABASE_BACKUP_DOWNLOAD)")
    public ResponseEntity<FileSystemResource> download(@Valid @RequestBody BackupIdRequestDto request) throws IOException {
        FileSystemResource resource = service.artifact(request.backupId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(service.downloadName(request.backupId())).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
