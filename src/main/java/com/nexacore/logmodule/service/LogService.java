package com.nexacore.logmodule.service;

import com.nexacore.logmodule.dto.LogContextDto;
import com.nexacore.logmodule.entity.LogApiAccessLog;
import com.nexacore.logmodule.entity.LogAuditLog;
import com.nexacore.logmodule.entity.LogErrorLog;
import com.nexacore.logmodule.repository.ApiAccessLogRepository;
import com.nexacore.logmodule.repository.AuditLogRepository;
import com.nexacore.logmodule.repository.ErrorLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    private final ApiAccessLogRepository apiAccessLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final ErrorLogRepository errorLogRepository;

    public LogService(ApiAccessLogRepository apiAccessLogRepository,
                      AuditLogRepository auditLogRepository,
                      ErrorLogRepository errorLogRepository) {
        this.apiAccessLogRepository = apiAccessLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional(transactionManager = "logTransactionManager")
    public void writeApiAccessLog(String method,
                                  String uri,
                                  int status,
                                  String username,
                                  String requestBody,
                                  String responseBody) {
        writeApiAccessLog(method, uri, status, username, requestBody, responseBody, null);
    }

    @Transactional(transactionManager = "logTransactionManager")
    public void writeApiAccessLog(String method,
                                  String uri,
                                  int status,
                                  String username,
                                  String requestBody,
                                  String responseBody,
                                  LogContextDto context) {
        LogApiAccessLog log = new LogApiAccessLog();
        log.setMethod(method);
        log.setUri(uri);
        log.setStatus(status);
        log.setUsername(username);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);
        applyContext(log, context);

        apiAccessLogRepository.save(log);
    }

    @Transactional(transactionManager = "logTransactionManager")
    public void writeAuditLog(String username,
                              String action,
                              String entityName,
                              String entityId,
                              String details) {
        LogAuditLog log = new LogAuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setDetails(details);
        applyContext(log, null);

        auditLogRepository.save(log);
    }

    @Transactional(transactionManager = "logTransactionManager")
    public void writeErrorLog(String method,
                              String uri,
                              int status,
                              String username,
                              String errorType,
                              String message,
                              String requestBody,
                              String responseBody) {
        writeErrorLog(method, uri, status, username, errorType, message, requestBody, responseBody, null);
    }

    @Transactional(transactionManager = "logTransactionManager")
    public void writeErrorLog(String method,
                              String uri,
                              int status,
                              String username,
                              String errorType,
                              String message,
                              String requestBody,
                              String responseBody,
                              LogContextDto context) {
        LogErrorLog log = new LogErrorLog();
        log.setMethod(method);
        log.setUri(uri);
        log.setStatus(status);
        log.setUsername(username);
        log.setErrorType(errorType);
        log.setMessage(message);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);
        applyContext(log, context);

        errorLogRepository.save(log);
    }

    private void applyContext(LogApiAccessLog log, LogContextDto context) {
        if (context == null) {
            return;
        }
        log.setTraceId(context.getTraceId());
        log.setClientCode(context.getClientCode());
        log.setClientType(context.getClientType());
        log.setUserId(context.getUserId());
        log.setApiCode(context.getApiCode());
        log.setModuleCode(context.getModuleCode());
        log.setModuleName(context.getModuleName());
        log.setSubmoduleCode(context.getSubmoduleCode());
        log.setSubmoduleName(context.getSubmoduleName());
        log.setFeatureCode(context.getFeatureCode());
        log.setFeatureName(context.getFeatureName());
        log.setActionCode(context.getActionCode());
        log.setActionName(context.getActionName());
        log.setAccessMode(context.getAccessMode());
        log.setDecision(context.getDecision());
        log.setDenyReason(context.getDenyReason());
        log.setBusinessId(context.getBusinessId());
        log.setBranchId(context.getBranchId());
    }

    private void applyContext(LogErrorLog log, LogContextDto context) {
        if (context == null) {
            return;
        }
        log.setTraceId(context.getTraceId());
        log.setClientCode(context.getClientCode());
        log.setClientType(context.getClientType());
        log.setUserId(context.getUserId());
        log.setApiCode(context.getApiCode());
        log.setModuleCode(context.getModuleCode());
        log.setModuleName(context.getModuleName());
        log.setSubmoduleCode(context.getSubmoduleCode());
        log.setSubmoduleName(context.getSubmoduleName());
        log.setFeatureCode(context.getFeatureCode());
        log.setFeatureName(context.getFeatureName());
        log.setActionCode(context.getActionCode());
        log.setActionName(context.getActionName());
        log.setAccessMode(context.getAccessMode());
        log.setBusinessId(context.getBusinessId());
        log.setBranchId(context.getBranchId());
    }

    private void applyContext(LogAuditLog log, LogContextDto context) {
        if (context == null) {
            return;
        }
        log.setTraceId(context.getTraceId());
        log.setClientCode(context.getClientCode());
        log.setClientType(context.getClientType());
        log.setUserId(context.getUserId());
        log.setModuleCode(context.getModuleCode());
        log.setModuleName(context.getModuleName());
        log.setSubmoduleCode(context.getSubmoduleCode());
        log.setSubmoduleName(context.getSubmoduleName());
        log.setFeatureCode(context.getFeatureCode());
        log.setFeatureName(context.getFeatureName());
        log.setActionCode(context.getActionCode());
        log.setActionName(context.getActionName());
        log.setAccessMode(context.getAccessMode());
        log.setBusinessId(context.getBusinessId());
        log.setBranchId(context.getBranchId());
    }
}
