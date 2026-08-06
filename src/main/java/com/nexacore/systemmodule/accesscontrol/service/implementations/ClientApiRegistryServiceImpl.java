package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.accesscontrol.security.ClientSecuredApi;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientApiRegistryServiceImpl implements ClientApiRegistryService {

    private static final String SOURCE_ANNOTATION = "ANNOTATION";
    private static final String SOURCE_MANUAL = "MANUAL";

    private final ApiRegistryRepository apiRegistryRepository;
    private final AuthModuleGateway authModuleGateway;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysPrivApiRegistry api = requestDto.getId() == null
                ? apiRegistryRepository.findByApiCode(requireText(requestDto.getApiCode(), "apiCode"))
                .orElseGet(SysPrivApiRegistry::new)
                : apiRegistryRepository.findById(requestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("API registry not found: " + requestDto.getId()));

        api.setApiCode(requireText(requestDto.getApiCode(), "apiCode"));
        api.setHttpMethod(requireText(requestDto.getHttpMethod(), "httpMethod").toUpperCase());
        api.setPathPattern(requireText(requestDto.getPathPattern(), "pathPattern"));
        api.setModuleCode(requestDto.getModuleCode());
        api.setModuleName(requestDto.getModuleName());
        api.setSubmoduleCode(requestDto.getSubmoduleCode());
        api.setSubmoduleName(requestDto.getSubmoduleName());
        api.setFeatureTypeCode(requestDto.getFeatureTypeCode());
        api.setFeatureTypeName(requestDto.getFeatureTypeName());
        api.setFeatureCode(requestDto.getFeatureCode());
        api.setFeatureName(requestDto.getFeatureName());
        api.setActionCode(requestDto.getActionCode());
        api.setActionName(requestDto.getActionName());
        api.setRequiredPrivilegeCode(requestDto.getRequiredPrivilegeCode());
        api.setPublicApi(Boolean.TRUE.equals(requestDto.getPublicApi()));
        api.setClientAuthenticationRequirement(defaultText(requestDto.getClientAuthenticationRequirement(), "REQUIRED"));
        api.setUserAuthorizationRequirement(defaultText(requestDto.getUserAuthorizationRequirement(), "PRIVILEGE"));
        api.setDataScope(defaultText(requestDto.getDataScope(), "NONE"));
        api.setSource(api.getSource() == null ? SOURCE_MANUAL : api.getSource());
        api.setPriority(requestDto.getPriority() == null ? 0 : requestDto.getPriority());
        api.setActive(requestDto.getActive() == null || requestDto.getActive());
        if (api.getId() == null) {
            api.setCreatedBy(actorId);
        }
        api.setUpdatedBy(actorId);

        return ApiRegistryDto.fromEntity(apiRegistryRepository.save(api));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<ApiRegistryDto> list() {
        return apiRegistryRepository.findAll().stream()
                .map(ApiRegistryDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public ApiRegistrySyncReportDto syncFromAnnotations(String username) {
        Long actorId = authModuleGateway.getUserId(username);
        Map<String, AnnotationMapping> discovered = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            ClientSecuredApi annotation = findClientSecuredApi(handlerMethod);
            if (annotation == null) {
                return;
            }
            Set<String> paths = mappingInfo.getPatternValues();
            Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
            if (methods.isEmpty()) {
                conflicts.add(handlerMethod + " does not declare an explicit HTTP method");
                return;
            }
            paths.stream().map(this::normalizePath).sorted().forEach(path -> methods.stream().sorted().forEach(method -> {
                String apiCode = method + ":" + path;
                AnnotationMapping previous = discovered.putIfAbsent(apiCode, new AnnotationMapping(path, method, annotation));
                if (previous != null) {
                    conflicts.add("Duplicate mapping " + apiCode);
                }
            }));
        });

        Map<String, String> patternOwner = new java.util.HashMap<>();
        discovered.forEach((apiCode, mapping) -> {
            String shapeKey = mapping.method() + ":" + mapping.path()
                    .replaceAll("\\{[^/]+}", "{}")
                    .replaceAll("\\*+", "*");
            String previous = patternOwner.putIfAbsent(shapeKey, apiCode);
            if (previous != null && !previous.equals(apiCode)) {
                conflicts.add("Overlapping mappings " + previous + " and " + apiCode);
            }
        });

        int added = 0;
        int changed = 0;
        int unchanged = 0;
        Set<String> synchronizedCodes = new java.util.HashSet<>();
        for (Map.Entry<String, AnnotationMapping> entry : discovered.entrySet()) {
            try {
                SyncOutcome outcome = saveAnnotationMapping(entry.getValue(), actorId);
                synchronizedCodes.add(entry.getKey());
                if (outcome == SyncOutcome.ADDED) added++;
                else if (outcome == SyncOutcome.CHANGED) changed++;
                else unchanged++;
            } catch (IllegalArgumentException exception) {
                conflicts.add(entry.getKey() + ": " + exception.getMessage());
            }
        }

        int deactivated = 0;
        for (SysPrivApiRegistry existing : apiRegistryRepository.findBySource(SOURCE_ANNOTATION)) {
            if (existing.isActive() && !synchronizedCodes.contains(existing.getApiCode())) {
                existing.setActive(false);
                existing.setUpdatedBy(actorId);
                existing.setLastSynchronizedAt(LocalDateTime.now());
                apiRegistryRepository.save(existing);
                deactivated++;
            }
        }

        return ApiRegistrySyncReportDto.builder()
                .added(added).changed(changed).unchanged(unchanged).deactivated(deactivated)
                .conflicted(conflicts.size()).conflicts(List.copyOf(conflicts)).records(list())
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysPrivApiRegistry> resolve(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();
        return apiRegistryRepository.findByHttpMethodAndActiveTrue(method).stream()
                .filter(api -> pathMatcher.match(api.getPathPattern(), path))
                .max(Comparator.comparingInt(api -> specificity(api.getPathPattern())));
    }

    private int specificity(String pattern) {
        return pattern == null ? 0 : pattern.replace("*", "").length();
    }

    private ClientSecuredApi findClientSecuredApi(HandlerMethod handlerMethod) {
        ClientSecuredApi annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), ClientSecuredApi.class);
        return annotation == null
                ? AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), ClientSecuredApi.class)
                : annotation;
    }

    private SyncOutcome saveAnnotationMapping(AnnotationMapping mapping, Long actorId) {
        String path = mapping.path();
        String method = mapping.method();
        ClientSecuredApi annotation = mapping.annotation();
        String requiredPrivilegeCode = requiredPrivilegeCode(annotation);
        String apiCode = method + ":" + path;
        SysPrivApiRegistry api = apiRegistryRepository.findByApiCode(apiCode).orElseGet(SysPrivApiRegistry::new);
        boolean added = api.getId() == null;
        String before = fingerprint(api);
        api.setApiCode(apiCode);
        api.setHttpMethod(method);
        api.setPathPattern(path);
        api.setModuleCode(annotation.moduleCode());
        api.setModuleName(annotation.moduleName());
        api.setSubmoduleCode(annotation.submoduleCode());
        api.setSubmoduleName(annotation.submoduleName());
        api.setFeatureTypeCode(annotation.featureTypeCode());
        api.setFeatureTypeName(annotation.featureTypeName());
        api.setFeatureCode(annotation.featureCode());
        api.setFeatureName(annotation.featureName());
        api.setActionCode(annotation.actionCode());
        api.setActionName(annotation.actionName());
        api.setRequiredPrivilegeCode(requiredPrivilegeCode);
        api.setPublicApi(annotation.publicApi());
        api.setClientAuthenticationRequirement(annotation.clientAuthentication().name());
        api.setUserAuthorizationRequirement(annotation.userAuthorization().name());
        api.setDataScope(annotation.dataScope().name());
        api.setSource(SOURCE_ANNOTATION);
        api.setPriority(0);
        api.setLastSynchronizedAt(LocalDateTime.now());
        api.setActive(true);
        if (api.getId() == null) {
            api.setCreatedBy(actorId);
        }
        api.setUpdatedBy(actorId);
        apiRegistryRepository.save(api);
        return added ? SyncOutcome.ADDED : Objects.equals(before, fingerprint(api)) ? SyncOutcome.UNCHANGED : SyncOutcome.CHANGED;
    }

    private String requiredPrivilegeCode(ClientSecuredApi annotation) {
        if (annotation.publicApi() || annotation.userAuthorization() == com.nexacore.systemmodule.accesscontrol.security.UserAuthorizationRequirement.NONE) {
            return null;
        }
        if (annotation.requiredPrivilegeCode() != null && !annotation.requiredPrivilegeCode().isBlank()) {
            if (!annotation.requiredPrivilegeCode().matches("\\d{11}")) {
                throw new IllegalArgumentException("requiredPrivilegeCode must contain 11 digits");
            }
            return annotation.requiredPrivilegeCode();
        }
        String code = annotation.moduleCode() + annotation.submoduleCode() + annotation.featureTypeCode()
                + annotation.featureCode() + annotation.actionCode();
        if (!code.matches("\\d{11}")) {
            throw new IllegalArgumentException("privilege composition must contain 2+2+2+3+2 digits");
        }
        return code;
    }

    private String normalizePath(String path) {
        String normalized = path == null ? "" : path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String fingerprint(SysPrivApiRegistry api) {
        return String.join("|", nullSafe(api.getApiCode()), nullSafe(api.getHttpMethod()), nullSafe(api.getPathPattern()),
                nullSafe(api.getModuleCode()), nullSafe(api.getModuleName()),
                nullSafe(api.getSubmoduleCode()), nullSafe(api.getSubmoduleName()),
                nullSafe(api.getFeatureTypeCode()), nullSafe(api.getFeatureTypeName()),
                nullSafe(api.getFeatureCode()), nullSafe(api.getFeatureName()),
                nullSafe(api.getActionCode()), nullSafe(api.getActionName()), nullSafe(api.getRequiredPrivilegeCode()),
                Boolean.toString(api.isPublicApi()), nullSafe(api.getClientAuthenticationRequirement()),
                nullSafe(api.getUserAuthorizationRequirement()), nullSafe(api.getDataScope()),
                nullSafe(api.getSource()), Integer.toString(api.getPriority()),
                Boolean.toString(api.isActive()));
    }

    private String nullSafe(String value) { return value == null ? "" : value; }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private record AnnotationMapping(String path, String method, ClientSecuredApi annotation) {}
    private enum SyncOutcome { ADDED, CHANGED, UNCHANGED }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
