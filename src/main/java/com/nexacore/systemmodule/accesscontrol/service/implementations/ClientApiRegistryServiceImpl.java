package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncChangeDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryConflictDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.accesscontrol.security.ClientSecuredApi;
import com.nexacore.systemmodule.accesscontrol.security.ApiRouteMatcher;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.systemmodule.accesscontrol.security.AccessControlMetrics;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.nexacore.commonmodule.util.AssignmentVersion;

@Service
@RequiredArgsConstructor
public class ClientApiRegistryServiceImpl implements ClientApiRegistryService {

    private static final String SOURCE_ANNOTATION = "ANNOTATION";
    private static final String SOURCE_MANUAL = "MANUAL";

    private final ApiRegistryRepository apiRegistryRepository;
    private final AuthModuleGateway authModuleGateway;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ApiRouteMatcher apiRouteMatcher;
    private final AuthorizationDataCache authorizationDataCache;
    private final AccessControlMetrics accessControlMetrics;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysAccApiRegistry api = requestDto.getId() == null
                ? apiRegistryRepository.findByApiCode(requireText(requestDto.getApiCode(), "apiCode"))
                .orElseGet(SysAccApiRegistry::new)
                : apiRegistryRepository.findById(requestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("API registry not found: " + requestDto.getId()));
        if (api.getId() != null && !Objects.equals(api.getVersion(), requestDto.getVersion())) {
            throw new IllegalStateException("API registry entry changed; reload before saving");
        }

        api.setApiCode(requireText(requestDto.getApiCode(), "apiCode"));
        api.setHttpMethod(requireText(requestDto.getHttpMethod(), "httpMethod").toUpperCase());
        api.setPathPattern(normalizePath(requireText(requestDto.getPathPattern(), "pathPattern")));
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

        validateNoAmbiguousActiveRoute(api);

        ApiRegistryDto saved = ApiRegistryDto.fromEntity(apiRegistryRepository.saveAndFlush(api));
        authorizationDataCache.invalidateRegistryAfterCommit();
        return saved;
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
        Set<String> conflictedCodes = new java.util.HashSet<>();
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
                    conflictedCodes.add(apiCode);
                }
            }));
        });

        List<Map.Entry<String, AnnotationMapping>> mappings = new ArrayList<>(discovered.entrySet());
        for (int leftIndex = 0; leftIndex < mappings.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < mappings.size(); rightIndex++) {
                var left = mappings.get(leftIndex);
                var right = mappings.get(rightIndex);
                if (apiRouteMatcher.hasUnresolvedOverlap(asRegistry(left), asRegistry(right))) {
                    conflicts.add("Ambiguous overlapping mappings " + left.getKey() + " and " + right.getKey());
                    conflictedCodes.add(left.getKey());
                    conflictedCodes.add(right.getKey());
                }
            }
        }

        int added = 0;
        int changed = 0;
        int unchanged = 0;
        Set<String> synchronizedCodes = new java.util.HashSet<>();
        for (Map.Entry<String, AnnotationMapping> entry : discovered.entrySet()) {
            if (conflictedCodes.contains(entry.getKey())) continue;
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
        for (SysAccApiRegistry existing : apiRegistryRepository.findBySource(SOURCE_ANNOTATION)) {
            if (existing.isActive() && !synchronizedCodes.contains(existing.getApiCode())) {
                existing.setActive(false);
                existing.setUpdatedBy(actorId);
                existing.setLastSynchronizedAt(LocalDateTime.now());
                apiRegistryRepository.save(existing);
                deactivated++;
            }
        }

        authorizationDataCache.invalidateRegistryAfterCommit();
        return ApiRegistrySyncReportDto.builder()
                .added(added).changed(changed).unchanged(unchanged).deactivated(deactivated)
                .conflicted(conflicts.size()).conflicts(List.copyOf(conflicts)).records(list()).applied(true)
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ApiRegistrySyncReportDto previewSyncFromAnnotations() {
        Map<String, List<DiscoveredMapping>> candidates = new LinkedHashMap<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            ClientSecuredApi annotation=findClientSecuredApi(handlerMethod);
            if(annotation==null) return;
            Set<String> methods=mappingInfo.getMethodsCondition().getMethods().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
            if(methods.isEmpty()) {
                candidates.computeIfAbsent("UNDECLARED:"+handlerMethod,k->new ArrayList<>())
                        .add(new DiscoveredMapping("", "", annotation,handlerMethod.toString()));
                return;
            }
            mappingInfo.getPatternValues().stream().map(this::normalizePath).forEach(path->methods.forEach(method->
                    candidates.computeIfAbsent(method+":"+path,k->new ArrayList<>())
                            .add(new DiscoveredMapping(path,method,annotation,handlerMethod.toString()))));
        });
        List<ApiRegistryConflictDto> conflictDetails=new ArrayList<>();
        Set<String> conflicted=new java.util.HashSet<>();
        candidates.forEach((code,values)->{
            if(values.size()>1 || code.startsWith("UNDECLARED:")) {
                conflicted.add(code); conflictDetails.add(new ApiRegistryConflictDto(code,
                        values.stream().map(DiscoveredMapping::handler).toList(),
                        List.of(code.startsWith("UNDECLARED:") ? "Handler does not declare an explicit HTTP method" : "Multiple handlers declare the same method and path")));
            }
        });
        List<Map.Entry<String,List<DiscoveredMapping>>> unique=candidates.entrySet().stream().filter(e->e.getValue().size()==1 && !e.getKey().startsWith("UNDECLARED:")).toList();
        for(int left=0;left<unique.size();left++) for(int right=left+1;right<unique.size();right++) {
            var a=unique.get(left); var b=unique.get(right);
            if(apiRouteMatcher.hasUnresolvedOverlap(asRegistry(a.getKey(),a.getValue().getFirst()),asRegistry(b.getKey(),b.getValue().getFirst()))) {
                String reason="Unresolved route-precedence overlap between "+a.getKey()+" and "+b.getKey();
                conflicted.add(a.getKey()); conflicted.add(b.getKey());
                conflictDetails.add(new ApiRegistryConflictDto(a.getKey()+" <> "+b.getKey(),
                        List.of(a.getValue().getFirst().handler(),b.getValue().getFirst().handler()),List.of(reason)));
            }
        }
        Map<String,SysAccApiRegistry> existing=apiRegistryRepository.findAll().stream().collect(java.util.stream.Collectors.toMap(SysAccApiRegistry::getApiCode,value->value,(a,b)->a,LinkedHashMap::new));
        List<ApiRegistrySyncChangeDto> changes=new ArrayList<>(); int added=0,changed=0,unchanged=0,deactivated=0;
        for(var entry:unique) {
            if(conflicted.contains(entry.getKey())) continue;
            SysAccApiRegistry proposed=asRegistry(entry.getKey(),entry.getValue().getFirst());
            SysAccApiRegistry before=existing.get(entry.getKey());
            if(before==null) { added++; changes.add(new ApiRegistrySyncChangeDto("ADDED",entry.getKey(),null,ApiRegistryDto.fromEntity(proposed))); }
            else if(!Objects.equals(fingerprint(before),fingerprint(proposed))) { changed++; changes.add(new ApiRegistrySyncChangeDto("CHANGED",entry.getKey(),ApiRegistryDto.fromEntity(before),ApiRegistryDto.fromEntity(proposed))); }
            else unchanged++;
        }
        Set<String> discovered=candidates.keySet();
        for(SysAccApiRegistry before:existing.values()) if(SOURCE_ANNOTATION.equals(before.getSource()) && before.isActive() && !discovered.contains(before.getApiCode())) {
            deactivated++; SysAccApiRegistry after=copyForPreview(before); after.setActive(false);
            changes.add(new ApiRegistrySyncChangeDto("DEACTIVATED",before.getApiCode(),ApiRegistryDto.fromEntity(before),ApiRegistryDto.fromEntity(after)));
        }
        List<String> versionParts=new ArrayList<>(changes.stream().map(change->change.kind()+":"+change.apiCode()+":"+(change.before()==null?"":change.before().getVersion())).toList());
        versionParts.addAll(conflictDetails.stream().map(ApiRegistryConflictDto::apiCode).sorted().toList());
        return ApiRegistrySyncReportDto.builder().added(added).changed(changed).unchanged(unchanged).deactivated(deactivated)
                .conflicted(conflicted.size()).conflicts(conflictDetails.stream().flatMap(item->item.reasons().stream()).toList())
                .conflictDetails(conflictDetails).changes(changes).previewVersion(AssignmentVersion.of(versionParts)).applied(false).build();
    }

    @Override
    public ApiRegistrySyncReportDto syncFromAnnotations(String username,String previewVersion) {
        ApiRegistrySyncReportDto preview=previewSyncFromAnnotations();
        if(previewVersion==null || !previewVersion.equals(preview.getPreviewVersion())) throw new IllegalStateException("Synchronization preview is stale; preview again");
        if(preview.getConflicted()>0) throw new IllegalStateException("Ambiguous registry mappings must be resolved before synchronization");
        ApiRegistrySyncReportDto applied=syncFromAnnotations(username); applied.setPreviewVersion(previewVersion); applied.setChanges(preview.getChanges());
        return applied;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysAccApiRegistry> resolve(HttpServletRequest request) {
        long startedAt = System.nanoTime();
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();
        try {
            Optional<SysAccApiRegistry> resolved = apiRouteMatcher.resolve(authorizationDataCache.registryMappings(method,
                    () -> apiRegistryRepository.findByHttpMethodAndActiveTrue(method)), path);
            accessControlMetrics.recordRegistryResolution(System.nanoTime() - startedAt,
                    resolved.isPresent() ? "matched" : "unregistered");
            return resolved;
        } catch (RuntimeException exception) {
            accessControlMetrics.recordRegistryResolution(System.nanoTime() - startedAt, "error");
            throw exception;
        }
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
        SysAccApiRegistry api = apiRegistryRepository.findByApiCode(apiCode).orElseGet(SysAccApiRegistry::new);
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
        api.setPriority(annotation.priority());
        api.setLastSynchronizedAt(LocalDateTime.now());
        api.setActive(true);
        if (api.getId() == null) {
            api.setCreatedBy(actorId);
        }
        api.setUpdatedBy(actorId);
        validateNoAmbiguousActiveRoute(api);
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

    private void validateNoAmbiguousActiveRoute(SysAccApiRegistry candidate) {
        if (!candidate.isActive()) return;
        for (SysAccApiRegistry existing : apiRegistryRepository.findByHttpMethodAndActiveTrue(candidate.getHttpMethod())) {
            if (Objects.equals(existing.getId(), candidate.getId())) continue;
            if (existing.getPathPattern().equals(candidate.getPathPattern())) {
                throw new IllegalArgumentException("An active registry entry already exists for method and path");
            }
            if (apiRouteMatcher.hasUnresolvedOverlap(existing, candidate)) {
                throw new IllegalArgumentException("Route has unresolved precedence overlap with " + existing.getApiCode());
            }
        }
    }

    private SysAccApiRegistry asRegistry(Map.Entry<String, AnnotationMapping> entry) {
        AnnotationMapping mapping = entry.getValue();
        return SysAccApiRegistry.builder()
                .apiCode(entry.getKey()).httpMethod(mapping.method()).pathPattern(mapping.path())
                .priority(mapping.annotation().priority()).active(true).build();
    }

    private SysAccApiRegistry asRegistry(String apiCode,DiscoveredMapping mapping) {
        ClientSecuredApi annotation=mapping.annotation();
        return SysAccApiRegistry.builder().apiCode(apiCode).httpMethod(mapping.method()).pathPattern(mapping.path())
                .moduleCode(annotation.moduleCode()).moduleName(annotation.moduleName()).submoduleCode(annotation.submoduleCode()).submoduleName(annotation.submoduleName())
                .featureTypeCode(annotation.featureTypeCode()).featureTypeName(annotation.featureTypeName()).featureCode(annotation.featureCode()).featureName(annotation.featureName())
                .actionCode(annotation.actionCode()).actionName(annotation.actionName()).requiredPrivilegeCode(requiredPrivilegeCode(annotation))
                .publicApi(annotation.publicApi()).clientAuthenticationRequirement(annotation.clientAuthentication().name())
                .userAuthorizationRequirement(annotation.userAuthorization().name()).dataScope(annotation.dataScope().name())
                .source(SOURCE_ANNOTATION).priority(annotation.priority()).active(true).version(0L).build();
    }

    private SysAccApiRegistry copyForPreview(SysAccApiRegistry source) {
        SysAccApiRegistry copy=SysAccApiRegistry.builder().id(source.getId()).version(source.getVersion()).apiCode(source.getApiCode()).httpMethod(source.getHttpMethod())
                .pathPattern(source.getPathPattern()).requiredPrivilegeCode(source.getRequiredPrivilegeCode()).publicApi(source.isPublicApi())
                .clientAuthenticationRequirement(source.getClientAuthenticationRequirement()).userAuthorizationRequirement(source.getUserAuthorizationRequirement())
                .dataScope(source.getDataScope()).source(source.getSource()).priority(source.getPriority()).active(source.isActive()).build();
        return copy;
    }

    private String fingerprint(SysAccApiRegistry api) {
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
    private record DiscoveredMapping(String path,String method,ClientSecuredApi annotation,String handler) {}
    private enum SyncOutcome { ADDED, CHANGED, UNCHANGED }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
