package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ClientApiAccessFilterReportModeTest {

    @AfterEach
    void clearContext() {
        ClientApplicationContextHolder.clear();
    }

    @Test
    void reportModeRecordsDecisionButContinuesRequest() throws Exception {
        AccessControlProperties properties = properties(EnforcementMode.REPORT);
        ClientApiAccessFilter filter = filter(properties);
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enforceModeDeniesSameDecision() throws Exception {
        AccessControlProperties properties = properties(EnforcementMode.ENFORCE);
        ClientApiAccessFilter filter = filter(properties);
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CLIENT_API_NOT_ALLOWED");
    }

    private ClientApiAccessFilter filter(AccessControlProperties properties) {
        SysPrivApiRegistry api = protectedApi();
        ClientApiRegistryService registryService = new StubRegistryService(api);
        ClientAccessDecisionService decisionService = new StubDecisionService(api);
        return new ClientApiAccessFilter(
                registryService,
                decisionService,
                new ApiResponseJsonWriter(new ObjectMapper()),
                properties,
                new PublicRoutePolicy()
        );
    }

    private AccessControlProperties properties(EnforcementMode mode) {
        AccessControlProperties properties = new AccessControlProperties(new StandardEnvironment());
        properties.setEnforcementMode(mode);
        return properties;
    }

    private MockHttpServletRequest protectedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/person/view/all");
        request.setServletPath("/api/v1/person/view/all");
        return request;
    }

    private SysPrivApiRegistry protectedApi() {
        return SysPrivApiRegistry.builder()
                .id(1L)
                .apiCode("GET:/api/v1/person/view/all")
                .requiredPrivilegeCode("01010200101")
                .active(true)
                .build();
    }

    private record StubRegistryService(SysPrivApiRegistry api) implements ClientApiRegistryService {
        @Override public Optional<SysPrivApiRegistry> resolve(jakarta.servlet.http.HttpServletRequest request) { return Optional.of(api); }
        @Override public ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username) { throw new UnsupportedOperationException(); }
        @Override public List<ApiRegistryDto> list() { return List.of(); }
        @Override public ApiRegistrySyncReportDto syncFromAnnotations(String username) { throw new UnsupportedOperationException(); }
    }

    private record StubDecisionService(SysPrivApiRegistry api) implements ClientAccessDecisionService {
        @Override
        public ClientAccessDecisionDto decide(SysPrivClientApplication clientApplication, SysPrivApiRegistry ignored) {
            return ClientAccessDecisionDto.denied("CLIENT_API_NOT_ALLOWED", clientApplication, api);
        }

        @Override
        public Set<String> filterPrivilegeCodesForClient(SysPrivClientApplication clientApplication,
                                                         Set<String> userPrivilegeCodes) {
            return Set.of();
        }
    }
}
