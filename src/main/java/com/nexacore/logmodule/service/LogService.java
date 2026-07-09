package com.nexacore.logmodule.service;

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
        LogApiAccessLog log = new LogApiAccessLog();
        log.setMethod(method);
        log.setUri(uri);
        log.setStatus(status);
        log.setUsername(username);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);

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
        LogErrorLog log = new LogErrorLog();
        log.setMethod(method);
        log.setUri(uri);
        log.setStatus(status);
        log.setUsername(username);
        log.setErrorType(errorType);
        log.setMessage(message);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);

        errorLogRepository.save(log);
    }
}
