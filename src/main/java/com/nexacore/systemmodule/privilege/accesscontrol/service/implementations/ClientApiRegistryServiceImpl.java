package com.nexacore.systemmodule.privilege.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.security.ClientSecuredApi;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientApiRegistryService;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientApiRegistryServiceImpl implements ClientApiRegistryService {

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
    public List<ApiRegistryDto> syncFromAnnotations(String username) {
        Long actorId = authModuleGateway.getUserId(username);
        requestMappingHandlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            ClientSecuredApi annotation = findClientSecuredApi(handlerMethod);
            if (annotation == null) {
                return;
            }
            Set<String> paths = mappingInfo.getPatternValues();
            Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
            paths.forEach(path -> methods.forEach(method -> saveAnnotationMapping(path, method, annotation, actorId)));
        });

        return list();
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

    private void saveAnnotationMapping(String path, String method, ClientSecuredApi annotation, Long actorId) {
        String requiredPrivilegeCode = annotation.moduleCode()
                + annotation.submoduleCode()
                + annotation.featureTypeCode()
                + annotation.featureCode()
                + annotation.actionCode();
        String apiCode = method + ":" + path;
        SysPrivApiRegistry api = apiRegistryRepository.findByApiCode(apiCode).orElseGet(SysPrivApiRegistry::new);
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
        api.setRequiredPrivilegeCode(annotation.publicApi() ? null : requiredPrivilegeCode);
        api.setPublicApi(annotation.publicApi());
        api.setActive(true);
        if (api.getId() == null) {
            api.setCreatedBy(actorId);
        }
        api.setUpdatedBy(actorId);
        apiRegistryRepository.save(api);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
