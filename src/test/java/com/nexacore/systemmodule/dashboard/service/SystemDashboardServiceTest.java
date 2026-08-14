package com.nexacore.systemmodule.dashboard.service;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.repository.*;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.privilege.security.PrivilegeAuthorizer;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SystemDashboardServiceTest {
    private final PrivilegeAuthorizer privileges=mock(PrivilegeAuthorizer.class);
    private final ClientApplicationRepository clients=mock(ClientApplicationRepository.class);
    private final TenantRepository tenants=mock(TenantRepository.class);
    private final LicenseSubscriptionRepository licenses=mock(LicenseSubscriptionRepository.class);
    private final BackupJobRepository backups=mock(BackupJobRepository.class);
    private final ApiRegistryRepository apis=mock(ApiRegistryRepository.class);
    private final ClientApiRegistryService apiService=mock(ClientApiRegistryService.class);
    private final AccessControlProperties properties=mock(AccessControlProperties.class);
    private final DataScopeService scope=mock(DataScopeService.class);
    private final SystemDashboardService service=new SystemDashboardService(privileges,clients,tenants,licenses,backups,apis,apiService,properties,scope);
    private final Authentication authentication=mock(Authentication.class);

    @Test void omitsEveryUnauthorizedModuleWithoutQueryingIt(){
        var result=service.aggregate(authentication);
        assertThat(result.metrics()).isEmpty(); assertThat(result.diagnostics()).isNull();
        verifyNoInteractions(clients,tenants,licenses,backups,apis,apiService,scope);
    }
    @Test void returnsOnlyAuthorizedClientMetric(){
        when(privileges.has(authentication,BootstrapAdministrationPrivileges.CLIENT_APPLICATION_VIEW)).thenReturn(true);
        when(clients.countByStatus(any())).thenReturn(4L);
        var result=service.aggregate(authentication);
        assertThat(result.metrics()).containsOnlyKeys("activeClients"); assertThat(result.metrics().get("activeClients").value()).isEqualTo(4);
        assertThat(result.diagnostics()).isNull(); verifyNoInteractions(tenants,licenses,backups,apis,apiService,scope);
    }
}
