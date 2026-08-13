package com.nexacore.systemmodule.accesscontrol.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAdminWebApiContractTest {

    private static final Map<String, String> REQUIRED_APIS = Map.ofEntries(
            Map.entry("POST:/api/v1/auth/application-context", "AUTHENTICATED"),
            Map.entry("POST:/api/v1/auth/session-status", "AUTHENTICATED"),
            Map.entry("POST:/api/v1/auth/logout", "AUTHENTICATED"),
            Map.entry("POST:/api/v1/system/client-app/list", "11020100101"),
            Map.entry("POST:/api/v1/system/client-app/detail", "11020100101"),
            Map.entry("POST:/api/v1/system/client-app/save", "11020100187"),
            Map.entry("POST:/api/v1/system/api-registry/list", "11020100601"),
            Map.entry("POST:/api/v1/system/api-registry/inventory", "11020100601"),
            Map.entry("POST:/api/v1/system/api-registry/save", "11020100687"),
            Map.entry("POST:/api/v1/system/api-registry/sync", "11020100680"),
            Map.entry("POST:/api/v1/system/user/list", "11020100701"),
            Map.entry("POST:/api/v1/system/user/save", "11020100787"),
            Map.entry("POST:/api/v1/system/user/assign-roles", "11020100781"),
            Map.entry("POST:/api/v1/system/role/list", "11020100801"),
            Map.entry("POST:/api/v1/system/role/save", "11020100887"),
            Map.entry("POST:/api/v1/system/tenants/list", "11020100901"),
            Map.entry("POST:/api/v1/system/privilege/list", "11010100101"),
            Map.entry("POST:/api/v1/system/layout/profile/list", "11040100101"),
            Map.entry("POST:/api/v1/system/layout/navigation/tree/admin", "11040100101"),
            Map.entry("POST:/api/v1/system/license/plan/list", "11030199901"),
            Map.entry("POST:/api/v1/system/license/subscription/list", "11030199901"),
            Map.entry("POST:/api/v1/system/backup/list", "11060100101"),
            Map.entry("POST:/api/v1/system/backup/run", "11060100113"),
            Map.entry("POST:/api/v1/system/backup/download", "11060100104")
    );

    @Test
    void requiredFrontendApisExistWithReviewedAuthorizationMetadata() {
        Map<String, String> discovered = discoverMetadata();
        REQUIRED_APIS.forEach((api, privilege) -> assertThat(discovered)
                .as(api).containsEntry(api, privilege));
    }

    private Map<String, String> discoverMetadata() {
        Map<String, String> result = new LinkedHashMap<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.findCandidateComponents("com.nexacore").forEach(definition -> {
            try {
                Class<?> controller = Class.forName(definition.getBeanClassName());
                RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
                String basePath = firstPath(classMapping);
                for (Method method : controller.getDeclaredMethods()) {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    if (mapping == null || mapping.method().length == 0) continue;
                    ClientSecuredApi metadata = AnnotatedElementUtils.findMergedAnnotation(method, ClientSecuredApi.class);
                    if (metadata == null) continue;
                    String key = mapping.method()[0].name() + ":" + basePath + firstPath(mapping);
                    String privilege = metadata.userAuthorization() == UserAuthorizationRequirement.PRIVILEGE
                            ? metadata.requiredPrivilegeCode() : "AUTHENTICATED";
                    result.put(key, privilege);
                }
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException(exception);
            }
        });
        return result;
    }

    private String firstPath(RequestMapping mapping) {
        if (mapping == null) return "";
        if (mapping.path().length > 0) return mapping.path()[0];
        return mapping.value().length > 0 ? mapping.value()[0] : "";
    }
}
