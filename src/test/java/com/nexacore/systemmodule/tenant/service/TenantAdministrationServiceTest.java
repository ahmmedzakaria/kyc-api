package com.nexacore.systemmodule.tenant.service;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.tenant.dto.TenantDomainMutationRequest;
import com.nexacore.systemmodule.tenant.dto.TenantLifecycleRequest;
import com.nexacore.systemmodule.tenant.entity.SysTenant;
import com.nexacore.systemmodule.tenant.entity.SysTenantDomain;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.PlatformAdminAuditRepository;
import com.nexacore.systemmodule.tenant.repository.TenantDomainRepository;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdministrationServiceTest {
    @Mock TenantRepository tenantRepository;
    @Mock TenantDomainRepository domainRepository;
    @Mock TenantCodeNormalizer codeNormalizer;
    @Mock HostnameNormalizer hostnameNormalizer;
    @Mock TenantDomainResolver domainResolver;
    @Mock PlatformAdministrationAuditService auditService;
    @Mock PlatformAdminAuditRepository auditRepository;
    @Mock ClientApplicationTenantRepository clientTenantRepository;
    @Mock ClientLayoutProfileRepository clientLayoutRepository;
    @Mock LicenseSubscriptionRepository licenseSubscriptionRepository;
    @Mock AuthModuleGateway authModuleGateway;
    @InjectMocks TenantAdministrationService service;

    @Test
    void primaryDomainMustBeReplacedBeforeDeactivation() {
        SysTenant tenant = new SysTenant(); tenant.setId(7L);
        SysTenantDomain domain = new SysTenantDomain(); domain.setId(3L); domain.setTenant(tenant);
        domain.setHostname("tenant.example.test"); domain.setPrimaryDomain(true); domain.setActive(true);
        when(domainRepository.findByIdAndTenantId(3L, 7L)).thenReturn(Optional.of(domain));

        assertThatThrownBy(() -> service.deactivateDomain(new TenantDomainMutationRequest(7L, 3L, null, null), 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Primary domain");
    }

    @Test
    void lifecycleMutationRejectsStaleTenantVersion() {
        SysTenant tenant = new SysTenant(); tenant.setId(7L); tenant.setVersion(5L); tenant.setStatus(TenantStatus.ACTIVE);
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.transition(new TenantLifecycleRequest(7L, 4L, "maintenance"), TenantStatus.SUSPENDED, 11L))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
