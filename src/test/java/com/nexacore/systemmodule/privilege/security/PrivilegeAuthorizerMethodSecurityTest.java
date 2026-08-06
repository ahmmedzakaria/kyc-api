package com.nexacore.systemmodule.privilege.security;

import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrivilegeAuthorizerMethodSecurityTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesOrdinaryUserAndAllowsBootstrapAdministratorWithoutApiRegistry() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            SecuredAdministrationService service = context.getBean(SecuredAdministrationService.class);

            authenticate("ordinary-user");
            assertThatThrownBy(service::administer)
                    .isInstanceOf(AuthorizationDeniedException.class);

            authenticate("bootstrap-admin");
            assertThat(service.administer()).isEqualTo("allowed");
        }
    }

    private void authenticate(String username) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                username,
                "not-used",
                Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {

        @Bean
        PrivilegeModuleGateway privilegeModuleGateway() {
            return new StubPrivilegeModuleGateway();
        }

        @Bean
        PrivilegeAuthorizer privilegeAuthorizer(PrivilegeModuleGateway gateway) {
            return new PrivilegeAuthorizer(gateway);
        }

        @Bean
        SecuredAdministrationService securedAdministrationService() {
            return new SecuredAdministrationService();
        }
    }

    static class SecuredAdministrationService {

        @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_MANAGE)")
        public String administer() {
            return "allowed";
        }
    }

    static class StubPrivilegeModuleGateway implements PrivilegeModuleGateway {

        @Override
        public String buildPrivilegeCode(String moduleCode,
                                         String submoduleCode,
                                         String featureTypeCode,
                                         String featureCode,
                                         String actionCode) {
            return moduleCode + submoduleCode + featureTypeCode + featureCode + actionCode;
        }

        @Override
        public boolean hasPrivilege(String username, String privilegeCode) {
            return "bootstrap-admin".equals(username)
                    && BootstrapAdministrationPrivileges.CLIENT_APPLICATION_MANAGE.equals(privilegeCode);
        }

        @Override
        public boolean hasPrivilege(String username,
                                    String moduleCode,
                                    String submoduleCode,
                                    String featureTypeCode,
                                    String featureCode,
                                    String actionCode) {
            return hasPrivilege(username, buildPrivilegeCode(
                    moduleCode, submoduleCode, featureTypeCode, featureCode, actionCode
            ));
        }

        @Override
        public Set<String> getPrivilegeCodes(String username) {
            return "bootstrap-admin".equals(username)
                    ? Set.of(BootstrapAdministrationPrivileges.CLIENT_APPLICATION_MANAGE)
                    : Set.of();
        }
    }
}
