package com.nexacore.logmodule.service;

import com.nexacore.logmodule.dto.LogApiAccessLogDto;
import com.nexacore.logmodule.dto.LogAuditLogDto;
import com.nexacore.logmodule.dto.LogErrorLogDto;
import com.nexacore.logmodule.dto.LogListRequestDto;
import com.nexacore.logmodule.dto.LogPageDto;
import com.nexacore.logmodule.entity.LogApiAccessLog;
import com.nexacore.logmodule.entity.LogAuditLog;
import com.nexacore.logmodule.entity.LogErrorLog;
import com.nexacore.logmodule.repository.ApiAccessLogRepository;
import com.nexacore.logmodule.repository.AuditLogRepository;
import com.nexacore.logmodule.repository.ErrorLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Read-only search/pagination over the three log tables — the counterpart to
 * {@link LogService}'s write-only methods. Every query runs against
 * {@code logTransactionManager} since these entities live in a separate
 * EntityManagerFactory (own physical database, see LogDbConfig).
 */
@Service
public class LogQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ErrorLogRepository errorLogRepository;
    private final ApiAccessLogRepository apiAccessLogRepository;
    private final AuditLogRepository auditLogRepository;

    public LogQueryService(ErrorLogRepository errorLogRepository,
                           ApiAccessLogRepository apiAccessLogRepository,
                           AuditLogRepository auditLogRepository) {
        this.errorLogRepository = errorLogRepository;
        this.apiAccessLogRepository = apiAccessLogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(transactionManager = "logTransactionManager", readOnly = true)
    public LogPageDto<LogErrorLogDto> listErrorLogs(LogListRequestDto request) {
        Page<LogErrorLog> page = errorLogRepository.findAll(errorSpec(request), pageable(request));
        return toPageDto(page, LogQueryService::toDto);
    }

    @Transactional(transactionManager = "logTransactionManager", readOnly = true)
    public LogPageDto<LogApiAccessLogDto> listAccessLogs(LogListRequestDto request) {
        Page<LogApiAccessLog> page = apiAccessLogRepository.findAll(accessSpec(request), pageable(request));
        return toPageDto(page, LogQueryService::toDto);
    }

    @Transactional(transactionManager = "logTransactionManager", readOnly = true)
    public LogPageDto<LogAuditLogDto> listAuditLogs(LogListRequestDto request) {
        Page<LogAuditLog> page = auditLogRepository.findAll(auditSpec(request), pageable(request));
        return toPageDto(page, LogQueryService::toDto);
    }

    private PageRequest pageable(LogListRequestDto request) {
        int page = request.page() == null || request.page() < 0 ? 0 : request.page();
        int size = request.pageSize() == null || request.pageSize() <= 0 ? DEFAULT_PAGE_SIZE : request.pageSize();
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Specification<LogErrorLog> errorSpec(LogListRequestDto request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = commonPredicates(root, cb, request);
            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }
            if (request.errorType() != null && !request.errorType().isBlank()) {
                predicates.add(cb.equal(root.get("errorType"), request.errorType()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<LogApiAccessLog> accessSpec(LogListRequestDto request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = commonPredicates(root, cb, request);
            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<LogAuditLog> auditSpec(LogListRequestDto request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = commonPredicates(root, cb, request);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Filters shared by all three entities — same field names on every one. */
    private List<Predicate> commonPredicates(Root<?> root, jakarta.persistence.criteria.CriteriaBuilder cb, LogListRequestDto request) {
        List<Predicate> predicates = new ArrayList<>();
        if (request.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), request.from()));
        }
        if (request.to() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), request.to()));
        }
        if (request.username() != null && !request.username().isBlank()) {
            predicates.add(cb.equal(root.get("username"), request.username()));
        }
        if (request.moduleCode() != null && !request.moduleCode().isBlank()) {
            predicates.add(cb.equal(root.get("moduleCode"), request.moduleCode()));
        }
        if (request.traceId() != null && !request.traceId().isBlank()) {
            predicates.add(cb.equal(root.get("traceId"), request.traceId()));
        }
        return predicates;
    }

    private <E, D> LogPageDto<D> toPageDto(Page<E> page, Function<E, D> mapper) {
        return LogPageDto.<D>builder()
                .items(page.getContent().stream().map(mapper).toList())
                .total(page.getTotalElements())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .build();
    }

    private static LogErrorLogDto toDto(LogErrorLog e) {
        return LogErrorLogDto.builder()
                .id(e.getId())
                .method(e.getMethod())
                .uri(e.getUri())
                .status(e.getStatus())
                .traceId(e.getTraceId())
                .clientCode(e.getClientCode())
                .clientType(e.getClientType())
                .userId(e.getUserId())
                .tenantId(e.getTenantId())
                .username(e.getUsername())
                .apiCode(e.getApiCode())
                .moduleCode(e.getModuleCode())
                .moduleName(e.getModuleName())
                .submoduleCode(e.getSubmoduleCode())
                .submoduleName(e.getSubmoduleName())
                .featureCode(e.getFeatureCode())
                .featureName(e.getFeatureName())
                .actionCode(e.getActionCode())
                .actionName(e.getActionName())
                .accessMode(e.getAccessMode())
                .businessId(e.getBusinessId())
                .branchId(e.getBranchId())
                .errorType(e.getErrorType())
                .message(e.getMessage())
                .requestBody(e.getRequestBody())
                .responseBody(e.getResponseBody())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static LogApiAccessLogDto toDto(LogApiAccessLog e) {
        return LogApiAccessLogDto.builder()
                .id(e.getId())
                .method(e.getMethod())
                .uri(e.getUri())
                .status(e.getStatus())
                .traceId(e.getTraceId())
                .clientCode(e.getClientCode())
                .clientType(e.getClientType())
                .userId(e.getUserId())
                .tenantId(e.getTenantId())
                .username(e.getUsername())
                .apiCode(e.getApiCode())
                .moduleCode(e.getModuleCode())
                .moduleName(e.getModuleName())
                .submoduleCode(e.getSubmoduleCode())
                .submoduleName(e.getSubmoduleName())
                .featureCode(e.getFeatureCode())
                .featureName(e.getFeatureName())
                .actionCode(e.getActionCode())
                .actionName(e.getActionName())
                .accessMode(e.getAccessMode())
                .decision(e.getDecision())
                .denyReason(e.getDenyReason())
                .businessId(e.getBusinessId())
                .branchId(e.getBranchId())
                .requestBody(e.getRequestBody())
                .responseBody(e.getResponseBody())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static LogAuditLogDto toDto(LogAuditLog e) {
        return LogAuditLogDto.builder()
                .id(e.getId())
                .traceId(e.getTraceId())
                .clientCode(e.getClientCode())
                .clientType(e.getClientType())
                .userId(e.getUserId())
                .tenantId(e.getTenantId())
                .username(e.getUsername())
                .moduleCode(e.getModuleCode())
                .moduleName(e.getModuleName())
                .submoduleCode(e.getSubmoduleCode())
                .submoduleName(e.getSubmoduleName())
                .featureCode(e.getFeatureCode())
                .featureName(e.getFeatureName())
                .actionCode(e.getActionCode())
                .actionName(e.getActionName())
                .accessMode(e.getAccessMode())
                .action(e.getAction())
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
                .businessId(e.getBusinessId())
                .branchId(e.getBranchId())
                .details(e.getDetails())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
