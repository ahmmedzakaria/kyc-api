package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.security.*;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutBrandDto;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientLayoutAssignmentTenantIsolationTest {
    private final AtomicReference<Long> directIdTenant = new AtomicReference<>();

    @AfterEach void clearContext() {
        AuthenticatedRequestContextHolder.clear();
        EffectiveTenantAccessContextHolder.clear();
    }

    @Test void directIdUpdateCannotLoadAnotherTenantsAssignment() {
        ClientLayoutAssignmentServiceImpl service = service();
        authenticateAsTenant(2L);
        ClientLayoutAssignmentRequestDto request = new ClientLayoutAssignmentRequestDto();
        request.setId(77L); request.setClientApplicationId(3L); request.setLayoutProfileId(4L);

        assertThatThrownBy(() -> service.assign(request, "tenant-two-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        assertThat(directIdTenant.get()).isEqualTo(2L);
    }

    @Test void assignmentStoresTenantSpecificBrandingWithoutChangingGlobalProfile() {
        authenticateAsTenant(2L);
        ClientLayoutAssignmentRequestDto request = new ClientLayoutAssignmentRequestDto();
        request.setClientApplicationId(3L);
        request.setLayoutProfileId(4L);
        request.setBrandOverride(LayoutBrandDto.builder()
                .displayName("Tenant Two")
                .logoUrl("/tenant-two/logo.svg")
                .build());

        var result = service().assign(request, "tenant-two-admin");

        assertThat(result.getTenantId()).isEqualTo(2L);
        assertThat(result.getBrandOverride().getDisplayName()).isEqualTo("Tenant Two");
        assertThat(result.getBrandOverride().getLogoUrl()).isEqualTo("/tenant-two/logo.svg");
    }

    private ClientLayoutAssignmentServiceImpl service() {
        SysAccClientApplication client = new SysAccClientApplication(); client.setId(3L);
        SysLayoutProfile profile = new SysLayoutProfile(); profile.setId(4L);

        ClientLayoutProfileRepository assignments = proxy(ClientLayoutProfileRepository.class, (proxy, method, args) -> {
            if (method.getName().equals("existsByTenantIdAndClientApplicationIdAndDefaultProfileTrueAndActiveTrueAndIdNot")) return false;
            if (method.getName().equals("findByIdAndTenantId")) {
                directIdTenant.set((Long) args[1]);
                return Optional.empty();
            }
            if (method.getName().equals("save")) return args[0];
            throw new UnsupportedOperationException(method.getName());
        });
        ClientApplicationRepository clients = proxy(ClientApplicationRepository.class, (proxy, method, args) -> {
            if (method.getName().equals("findById")) return Optional.of(client);
            throw new UnsupportedOperationException(method.getName());
        });
        LayoutProfileRepository profiles = proxy(LayoutProfileRepository.class, (proxy, method, args) -> {
            if (method.getName().equals("findById")) return Optional.of(profile);
            throw new UnsupportedOperationException(method.getName());
        });
        AuthModuleGateway auth = proxy(AuthModuleGateway.class, (proxy, method, args) -> {
            if (method.getName().equals("getUserId")) return 9L;
            throw new UnsupportedOperationException(method.getName());
        });
        return new ClientLayoutAssignmentServiceImpl(assignments, clients, profiles, auth, new DataScopeService());
    }

    private void authenticateAsTenant(long tenantId) {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                9L, "tenant-admin", 3L, "WEB",
                Set.of(new UserScopeAssignment(tenantId, null, null)), "trace", Set.of()));
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
