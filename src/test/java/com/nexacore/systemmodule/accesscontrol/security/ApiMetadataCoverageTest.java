package com.nexacore.systemmodule.accesscontrol.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiMetadataCoverageTest {

    @Test
    void everyApplicationApiHasValidReviewedMetadataAndUniqueApiCode() {
        PublicRoutePolicy publicRoutePolicy = new PublicRoutePolicy();
        List<String> missingMetadata = new ArrayList<>();
        List<String> invalidPrivileges = new ArrayList<>();
        List<String> unapprovedPublicRoutes = new ArrayList<>();
        Set<String> apiCodes = new HashSet<>();
        List<String> duplicateApiCodes = new ArrayList<>();

        for (Class<?> controller : applicationControllers()) {
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            String basePath = firstPath(classMapping);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) continue;

                ClientSecuredApi metadata = AnnotatedElementUtils.findMergedAnnotation(method, ClientSecuredApi.class);
                String path = basePath + firstPath(methodMapping);
                String httpMethod = methodMapping.method().length == 0 ? "" : methodMapping.method()[0].name();
                String apiCode = httpMethod + ":" + path;
                if (metadata == null) {
                    missingMetadata.add(apiCode);
                    continue;
                }
                if (!apiCodes.add(apiCode)) duplicateApiCodes.add(apiCode);
                if (metadata.publicApi() && !publicRoutePolicy.isPublic(path)) unapprovedPublicRoutes.add(path);
                if (!metadata.publicApi() && metadata.userAuthorization() == UserAuthorizationRequirement.PRIVILEGE) {
                    String privilegeCode = metadata.requiredPrivilegeCode().isBlank()
                            ? metadata.moduleCode() + metadata.submoduleCode() + metadata.featureTypeCode()
                            + metadata.featureCode() + metadata.actionCode()
                            : metadata.requiredPrivilegeCode();
                    if (!privilegeCode.matches("\\d{11}")) invalidPrivileges.add(apiCode + "=" + privilegeCode);
                }
            }
        }

        assertThat(missingMetadata).isEmpty();
        assertThat(invalidPrivileges).isEmpty();
        assertThat(unapprovedPublicRoutes).isEmpty();
        assertThat(duplicateApiCodes).isEmpty();
        assertThat(apiCodes).isNotEmpty();
    }

    private List<Class<?>> applicationControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents("com.nexacore").stream()
                .<Class<?>>map(definition -> {
                    try {
                        return (Class<?>) Class.forName(definition.getBeanClassName());
                    } catch (ClassNotFoundException exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .toList();
    }

    private String firstPath(RequestMapping mapping) {
        if (mapping == null) return "";
        if (mapping.path().length > 0) return mapping.path()[0];
        return mapping.value().length > 0 ? mapping.value()[0] : "";
    }
}
