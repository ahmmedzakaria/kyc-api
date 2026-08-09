package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.appconfigmodule.HttpExceptionHandler;
import com.nexacore.authmodule.core.service.implementations.LogoutSessionService;
import com.nexacore.authmodule.security.config.AuthenticationProviderConfig;
import com.nexacore.authmodule.security.config.CorsProperties;
import com.nexacore.appconfigmodule.security.SecurityConfig;
import com.nexacore.authmodule.security.filter.JwtAuthenticationFilter;
import com.nexacore.authmodule.security.jwt.InvalidTokenTypeException;
import com.nexacore.authmodule.security.jwt.JwtAuthEntryPoint;
import com.nexacore.authmodule.security.jwt.JwtUtil;
import com.nexacore.commonmodule.i18n.service.interfaces.MessageLocalizationService;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.dto.AuthUserScopeAssignmentDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.controller.ClientApplicationController;
import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientPermissionService;
import com.nexacore.systemmodule.privilege.security.PrivilegeAuthorizer;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AccessControlFilterChainIntegrationTest.TestConfiguration.class)
@ActiveProfiles("filter-chain-test")
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration=60000", "jwt.refresh.expiration=120000",
        "jwt.issuer=test", "jwt.audience=test"
})
class AccessControlFilterChainIntegrationTest {
    private static final String REQUIRED = "01010200101";

    @Autowired private WebApplicationContext context;
    @Autowired private ClientCredentialService credentials;
    @Autowired private ClientApiRegistryService registryService;
    @Autowired private ClientAccessDecisionService decisionService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private LogoutSessionService logoutSessionService;
    @Autowired private AuthModuleGateway authModuleGateway;
    @Autowired private PrivilegeService privilegeService;
    @Autowired private PrivilegeModuleGateway privilegeModuleGateway;
    @Autowired private ClientApplicationService clientApplicationService;
    @Autowired private ClientPermissionService clientPermissionService;

    private MockMvc mvc;
    private SysAccClientApplication client;

    @BeforeEach
    void setUp() {
        Mockito.reset(credentials, registryService, decisionService, jwtUtil, logoutSessionService,
                authModuleGateway, privilegeService, privilegeModuleGateway,
                clientApplicationService, clientPermissionService);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        client = SysAccClientApplication.builder().id(3L).clientCode("client")
                .clientType(ClientApplicationType.INTERNAL_SERVICE).build();

        when(credentials.resolveActiveClient("client")).thenReturn(Optional.of(client));
        when(credentials.validateApiKey("client", "key")).thenReturn(Optional.of(client));
        when(credentials.resolveActiveClient("bad")).thenReturn(Optional.empty());
        when(credentials.resolveActiveClient("disabled")).thenReturn(Optional.empty());
        when(registryService.resolve(any())).thenAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            if (request.getRequestURI().contains("unregistered")) return Optional.empty();
            return Optional.of(api(request.getRequestURI()));
        });
        when(decisionService.decide(any(), any())).thenAnswer(invocation -> {
            SysAccClientApplication application = invocation.getArgument(0);
            SysAccApiRegistry api = invocation.getArgument(1);
            if (application == null) return ClientAccessDecisionDto.denied("CLIENT_REQUIRED", null, api);
            if (api.getPathPattern().contains("no-api-grant"))
                return ClientAccessDecisionDto.denied("CLIENT_API_NOT_ALLOWED", application, api);
            if (api.getPathPattern().contains("no-feature-grant"))
                return ClientAccessDecisionDto.denied("CLIENT_FEATURE_NOT_ALLOWED", application, api);
            return ClientAccessDecisionDto.allowed(application, api);
        });
        doAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("refresh".equals(token)) throw new InvalidTokenTypeException("refresh token");
            if ("invalid".equals(token)) throw new JwtException("invalid token");
            return null;
        }).when(jwtUtil).requireTokenType(anyString(), any());
        when(jwtUtil.extractUsername("access")).thenReturn("alice");
        when(jwtUtil.extractRoles("access")).thenReturn(List.of("ROLE_USER"));
        when(jwtUtil.extractIssuedAt("access")).thenReturn(Date.from(Instant.now()));
        when(logoutSessionService.isSessionActive(anyString(), any())).thenReturn(true);
        when(authModuleGateway.getUserAccess("alice")).thenReturn(AuthUserAccessDto.builder()
                .userId(7L).personId(8L)
                .scopeAssignments(Set.of(new AuthUserScopeAssignmentDto(1L, null, null))).build());
        when(privilegeService.getUserPrivilegeCodes("alice")).thenReturn(Set.of(REQUIRED));
        when(privilegeModuleGateway.hasPrivilege(anyString(), anyString())).thenReturn(false);
    }

    @Test void publicRegisteredApiWithoutJwtOrClientIsAllowed() throws Exception {
        mvc.perform(get("/api/v1/auth/test")).andExpect(status().isOk());
    }

    @Test void privateRegisteredApiWithoutClientIsRejected() throws Exception {
        mvc.perform(get("/api/test/private")).andExpect(error(401, "INVALID_CLIENT_CREDENTIALS"));
    }

    @Test void invalidClientCredentialIsRejected() throws Exception {
        mvc.perform(get("/api/test/private").header("X-Client-Code", "bad").header("X-API-Key", "wrong"))
                .andExpect(error(401, "INVALID_CLIENT_CREDENTIALS"));
    }

    @Test void disabledClientWithOtherwiseValidCredentialsIsRejected() throws Exception {
        mvc.perform(get("/api/test/private")
                        .header("X-Client-Code", "disabled")
                        .header("X-API-Key", "previously-valid-key"))
                .andExpect(error(401, "INVALID_CLIENT_CREDENTIALS"));

        verify(credentials, never()).validateApiKey("disabled", "previously-valid-key");
    }

    @Test void missingClientApiGrantIsRejected() throws Exception {
        mvc.perform(authenticated("/api/test/no-api-grant")).andExpect(error(403, "CLIENT_API_NOT_ALLOWED"));
    }

    @Test void missingClientFeatureGrantIsRejected() throws Exception {
        mvc.perform(authenticated("/api/test/no-feature-grant")).andExpect(error(403, "CLIENT_FEATURE_NOT_ALLOWED"));
    }

    @Test void missingAndInvalidAccessJwtUseAuthenticationContract() throws Exception {
        mvc.perform(clientRequest("/api/test/private")).andExpect(error(401, "AUTHENTICATION_REQUIRED"));
        mvc.perform(clientRequest("/api/test/private").header("Authorization", "Bearer invalid"))
                .andExpect(error(401, "AUTHENTICATION_REQUIRED"));
    }

    @Test void refreshTokenCannotBeUsedAsBearerAccessToken() throws Exception {
        mvc.perform(clientRequest("/api/test/private").header("Authorization", "Bearer refresh"))
                .andExpect(error(401, "INVALID_TOKEN_TYPE"));
    }

    @Test void userWithoutRequiredPrivilegeIsRejected() throws Exception {
        when(privilegeService.getUserPrivilegeCodes("alice")).thenReturn(Set.of());
        mvc.perform(authenticated("/api/test/private")).andExpect(error(403, "USER_PRIVILEGE_NOT_ALLOWED"));
    }

    @Test void userAndClientWithAllGrantsAreAllowed() throws Exception {
        mvc.perform(authenticated("/api/test/private")).andExpect(status().isOk());
    }

    @Test void unregisteredProtectedEndpointFailsClosed() throws Exception {
        mvc.perform(authenticated("/api/test/unregistered")).andExpect(error(403, "API_NOT_REGISTERED"));
    }

    @Test void clientOutsideAllowedIpIsRejected() throws Exception {
        client.setAllowedIps("10.0.0.0/8");
        mvc.perform(authenticated("/api/test/private").with(request -> {
            request.setRemoteAddr("203.0.113.9");
            return request;
        })).andExpect(error(403, "CLIENT_IP_NOT_ALLOWED"));
    }

    @Test void crossTenantDataRequestIsRejected() throws Exception {
        mvc.perform(authenticated("/api/test/other-tenant"))
                .andExpect(error(403, "DATA_SCOPE_NOT_ALLOWED"));
    }

    @Test void ordinaryUserCannotCallAdministrationEndpoint() throws Exception {
        mvc.perform(authenticated("/api/test/admin"))
                .andExpect(error(403, "USER_PRIVILEGE_NOT_ALLOWED"));
    }

    @Test void ordinaryUserCannotRotateAClientApiKey() throws Exception {
        mvc.perform(post("/api/v1/system/client-app/rotate-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Client-Code", "client")
                        .header("X-API-Key", "key")
                        .header("Authorization", "Bearer access")
                        .content("{\"clientApplicationId\":3}"))
                .andExpect(error(403, "USER_PRIVILEGE_NOT_ALLOWED"));

        verify(clientApplicationService, never()).rotateApiKey(any(), any(), anyString());
    }

    @Test void removedPrivilegeTakesEffectWhileTheLoginSessionRemainsActive() throws Exception {
        mvc.perform(authenticated("/api/test/private")).andExpect(status().isOk());

        when(privilegeService.getUserPrivilegeCodes("alice")).thenReturn(Set.of());

        mvc.perform(authenticated("/api/test/private"))
                .andExpect(error(403, "USER_PRIVILEGE_NOT_ALLOWED"));
        verify(logoutSessionService, org.mockito.Mockito.atLeast(2)).isSessionActive(anyString(), any());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder clientRequest(String path) {
        return get(path).accept(MediaType.APPLICATION_JSON)
                .header("X-Client-Code", "client").header("X-API-Key", "key");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticated(String path) {
        return clientRequest(path).header("Authorization", "Bearer access");
    }

    private org.springframework.test.web.servlet.ResultMatcher error(int statusCode, String code) {
        return result -> {
            status().is(statusCode).match(result);
            jsonPath("$.status").value("ERROR").match(result);
            jsonPath("$.statusCode").value(statusCode).match(result);
            jsonPath("$.message[0].code").value(code).match(result);
        };
    }

    private SysAccApiRegistry api(String path) {
        return SysAccApiRegistry.builder().id(20L).apiCode("GET:" + path).httpMethod("GET")
                .pathPattern(path).requiredPrivilegeCode(REQUIRED).active(true).build();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, HarnessController.class, ClientApplicationController.class, HttpExceptionHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean ApiResponseJsonWriter responseWriter(ObjectMapper mapper) { return new ApiResponseJsonWriter(mapper); }
        @Bean JwtAuthEntryPoint jwtAuthEntryPoint(ApiResponseJsonWriter writer) { return new JwtAuthEntryPoint(writer); }
        @Bean PublicRoutePolicy publicRoutePolicy() { return new PublicRoutePolicy(); }
        @Bean AccessControlProperties accessControlProperties() {
            AccessControlProperties properties = new AccessControlProperties(new org.springframework.mock.env.MockEnvironment());
            properties.setEnabled(true); properties.setEnforcementMode(EnforcementMode.ENFORCE); return properties;
        }
        @Bean CorsProperties corsProperties() { return new CorsProperties(); }
        @Bean ClientCredentialService credentials() { return Mockito.mock(ClientCredentialService.class); }
        @Bean ClientApiRegistryService registryService() { return Mockito.mock(ClientApiRegistryService.class); }
        @Bean ClientAccessDecisionService decisionService() { return Mockito.mock(ClientAccessDecisionService.class); }
        @Bean JwtUtil jwtUtil() { return Mockito.mock(JwtUtil.class); }
        @Bean LogoutSessionService logoutSessionService() { return Mockito.mock(LogoutSessionService.class); }
        @Bean AuthModuleGateway authModuleGateway() { return Mockito.mock(AuthModuleGateway.class); }
        @Bean PrivilegeService privilegeService() { return Mockito.mock(PrivilegeService.class); }
        @Bean PrivilegeModuleGateway privilegeModuleGateway() { return Mockito.mock(PrivilegeModuleGateway.class); }
        @Bean ClientApplicationService clientApplicationService() { return Mockito.mock(ClientApplicationService.class); }
        @Bean ClientPermissionService clientPermissionService() { return Mockito.mock(ClientPermissionService.class); }
        @Bean MessageLocalizationService localizationService() { return Mockito.mock(MessageLocalizationService.class); }
        @Bean AuthenticationProviderConfig authenticationProviderConfig() {
            AuthenticationProviderConfig config = Mockito.mock(AuthenticationProviderConfig.class);
            when(config.authenticationProvider()).thenReturn(Mockito.mock(AuthenticationProvider.class));
            return config;
        }
        @Bean ClientOriginPolicy clientOriginPolicy() { return new ClientOriginPolicy(); }
        @Bean ClientIpPolicy clientIpPolicy(AccessControlProperties properties) { return new ClientIpPolicy(properties); }
        @Bean ClientRateLimiter clientRateLimiter() { return (application, request) -> ClientRateLimitDecision.notLimited(); }
        @Bean AuthorizationEventEmitter authorizationEventEmitter(ObjectMapper mapper) {
            return new AuthorizationEventEmitter(Mockito.mock(ApplicationEventPublisher.class), mapper);
        }
        @Bean ClientApplicationAuthenticationFilter clientAuthenticationFilter(
                ClientCredentialService credentials, ClientOriginPolicy origins, ClientIpPolicy ips,
                ClientRateLimiter limiter, PublicRoutePolicy routes, ApiResponseJsonWriter writer,
                AccessControlProperties properties, AuthorizationEventEmitter events) {
            return new ClientApplicationAuthenticationFilter(credentials, origins, ips, limiter, routes, writer, properties, events);
        }
        @Bean ClientApiAccessFilter clientApiAccessFilter(ClientApiRegistryService registry,
                ClientAccessDecisionService decisions, ApiResponseJsonWriter writer,
                AccessControlProperties properties, PublicRoutePolicy routes) {
            return new ClientApiAccessFilter(registry, decisions, writer, properties, routes);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwt, LogoutSessionService logout) {
            return new JwtAuthenticationFilter(jwt, logout);
        }
        @Bean AuthenticatedRequestContextFilter authenticatedRequestContextFilter(
                AuthModuleGateway auth, PrivilegeService privileges) {
            return new AuthenticatedRequestContextFilter(auth, privileges);
        }
        @Bean UserPrivilegeApiAccessFilter userPrivilegeApiAccessFilter(ApiResponseJsonWriter writer,
                AccessControlProperties properties, PublicRoutePolicy routes) {
            return new UserPrivilegeApiAccessFilter(writer, properties, routes);
        }
        @Bean DataScopeService dataScopeService() { return new DataScopeService(); }
        @Bean(name = "privilegeAuthorizer") PrivilegeAuthorizer privilegeAuthorizer(PrivilegeModuleGateway gateway) {
            return new PrivilegeAuthorizer(gateway);
        }
    }

    @RestController
    @Profile("filter-chain-test")
    static class HarnessController {
        private final DataScopeService scopes;
        HarnessController(DataScopeService scopes) { this.scopes = scopes; }

        @GetMapping({"/api/v1/auth/test", "/api/test/private", "/api/test/no-api-grant",
                "/api/test/no-feature-grant", "/api/test/unregistered"})
        String allowed() { return "ok"; }

        @GetMapping("/api/test/other-tenant")
        ResponseEntity<ApiResponse<?>> otherTenant() {
            try {
                scopes.requireWritableScope(2L, null, null);
                return ResponseEntity.ok(ApiResponse.success("allowed"));
            } catch (DataScopeAccessDeniedException exception) {
                AccessControlError error = AccessControlError.DATA_SCOPE_NOT_ALLOWED;
                return ResponseEntity.status(403).body(ApiResponse.errorCode(
                        error.getStatus(), error.name(), error.getMessage()));
            }
        }

        @PreAuthorize("@privilegeAuthorizer.has(authentication, 'ADMIN')")
        @GetMapping("/api/test/admin")
        String admin() { return "admin"; }
    }
}
