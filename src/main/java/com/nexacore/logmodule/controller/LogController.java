package com.nexacore.logmodule.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.logmodule.dto.LogApiAccessLogDto;
import com.nexacore.logmodule.dto.LogAuditLogDto;
import com.nexacore.logmodule.dto.LogErrorLogDto;
import com.nexacore.logmodule.dto.LogListRequestDto;
import com.nexacore.logmodule.dto.LogPageDto;
import com.nexacore.logmodule.service.LogQueryService;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/log")
@RequiredArgsConstructor
public class LogController {

    private final LogQueryService service;

    @PostMapping("/error/list")
    @PrivilegeApi("09010100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LOG_ERROR_VIEW)")
    public ApiResponse<LogPageDto<LogErrorLogDto>> errorList(@RequestBody(required = false) LogListRequestDto request) {
        return ApiResponse.successCode(service.listErrorLogs(normalize(request)), "log.error.listed", "Error logs listed");
    }

    @PostMapping("/access/list")
    @PrivilegeApi("09010100201")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LOG_ACCESS_VIEW)")
    public ApiResponse<LogPageDto<LogApiAccessLogDto>> accessList(@RequestBody(required = false) LogListRequestDto request) {
        return ApiResponse.successCode(service.listAccessLogs(normalize(request)), "log.access.listed", "Access logs listed");
    }

    @PostMapping("/audit/list")
    @PrivilegeApi("09010100301")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LOG_AUDIT_VIEW)")
    public ApiResponse<LogPageDto<LogAuditLogDto>> auditList(@RequestBody(required = false) LogListRequestDto request) {
        return ApiResponse.successCode(service.listAuditLogs(normalize(request)), "log.audit.listed", "Audit logs listed");
    }

    private LogListRequestDto normalize(LogListRequestDto request) {
        return request != null ? request : LogListRequestDto.empty();
    }
}
