package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.dto.ApiInventoryItemDto;
import com.nexacore.systemmodule.accesscontrol.security.ClientSecuredApi;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ApiInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiInventoryServiceImpl implements ApiInventoryService {

    private static final String APPLICATION_PACKAGE = "com.nexacore.";
    private static final String API_PATH_PREFIX = "/api/";
    private static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public List<ApiInventoryItemDto> inventory() {
        List<ApiInventoryItemDto> inventory = new ArrayList<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!isApplicationApi(handler)) {
                return;
            }

            ClientSecuredApi metadata = findAccessMetadata(handler);
            Set<String> paths = mapping.getPatternValues();
            List<String> methods = httpMethods(mapping);

            paths.stream()
                    .filter(path -> path.startsWith(API_PATH_PREFIX))
                    .forEach(path -> methods.forEach(method -> inventory.add(toItem(method, path, handler, metadata))));
        });

        return inventory.stream()
                .sorted(Comparator.comparing(ApiInventoryItemDto::getPathPattern)
                        .thenComparing(ApiInventoryItemDto::getHttpMethod)
                        .thenComparing(ApiInventoryItemDto::getControllerClass)
                        .thenComparing(ApiInventoryItemDto::getHandlerMethod))
                .toList();
    }

    private boolean isApplicationApi(HandlerMethod handler) {
        Package handlerPackage = handler.getBeanType().getPackage();
        return handlerPackage != null && handlerPackage.getName().startsWith(APPLICATION_PACKAGE);
    }

    private List<String> httpMethods(RequestMappingInfo mapping) {
        Set<RequestMethod> declaredMethods = mapping.getMethodsCondition().getMethods();
        if (!declaredMethods.isEmpty()) {
            return declaredMethods.stream().map(Enum::name).sorted().toList();
        }
        return List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.HEAD.name()
        );
    }

    private ClientSecuredApi findAccessMetadata(HandlerMethod handler) {
        ClientSecuredApi annotation = AnnotatedElementUtils.findMergedAnnotation(
                handler.getMethod(),
                ClientSecuredApi.class
        );
        return annotation == null
                ? AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), ClientSecuredApi.class)
                : annotation;
    }

    private ApiInventoryItemDto toItem(
            String httpMethod,
            String path,
            HandlerMethod handler,
            ClientSecuredApi metadata
    ) {
        String requiredPrivilegeCode = metadata == null || metadata.publicApi()
                ? null
                : metadata.moduleCode()
                + metadata.submoduleCode()
                + metadata.featureTypeCode()
                + metadata.featureCode()
                + metadata.actionCode();

        return ApiInventoryItemDto.builder()
                .apiCode(httpMethod + ":" + path)
                .httpMethod(httpMethod)
                .pathPattern(path)
                .controllerClass(handler.getBeanType().getName())
                .handlerMethod(handler.getMethod().getName())
                .sourceModule(sourceModule(handler.getBeanType()))
                .accessMetadataDeclared(metadata != null)
                .publicApi(metadata == null ? null : metadata.publicApi())
                .moduleCode(metadata == null ? null : metadata.moduleCode())
                .submoduleCode(metadata == null ? null : metadata.submoduleCode())
                .featureTypeCode(metadata == null ? null : metadata.featureTypeCode())
                .featureCode(metadata == null ? null : metadata.featureCode())
                .actionCode(metadata == null ? null : metadata.actionCode())
                .requiredPrivilegeCode(requiredPrivilegeCode)
                .intendedClientTypes(List.of())
                .dataScope(REVIEW_REQUIRED)
                .reviewStatus(REVIEW_REQUIRED)
                .build();
    }

    private String sourceModule(Class<?> beanType) {
        String packageName = beanType.getPackageName();
        String relativeName = packageName.substring(APPLICATION_PACKAGE.length());
        int separator = relativeName.indexOf('.');
        return separator < 0 ? relativeName : relativeName.substring(0, separator);
    }
}
