package com.nexacore.systemmodule.dashboard.service;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.*;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import com.nexacore.systemmodule.dashboard.dto.*;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.privilege.security.PrivilegeAuthorizer;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class SystemDashboardService {
    private final PrivilegeAuthorizer privileges; private final ClientApplicationRepository clients;
    private final TenantRepository tenants; private final LicenseSubscriptionRepository licenses;
    private final BackupJobRepository backups; private final ApiRegistryRepository apis;
    private final ClientApiRegistryService apiService; private final AccessControlProperties accessControl;
    private final DataScopeService scope;

    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public DashboardAggregateDto aggregate(Authentication authentication){
        Map<String,DashboardMetricDto> metrics=new LinkedHashMap<>(); DashboardDiagnosticsDto diagnostics=null;
        if(has(authentication,BootstrapAdministrationPrivileges.CLIENT_APPLICATION_VIEW)){
            long value=clients.countByStatus(ClientApplicationStatus.ACTIVE); metrics.put("activeClients",metric(value,"NONE",null,"/access-control/client-applications"));
        }
        if(has(authentication,BootstrapAdministrationPrivileges.TENANT_VIEW)){
            long value=tenants.countByStatus(TenantStatus.PENDING); metrics.put("pendingTenants",metric(value,value>0?"WARNING":"NONE",value>0?"TENANT_ONBOARDING_PENDING":null,"/access-control/tenants"));
        }
        if(has(authentication,BootstrapAdministrationPrivileges.LICENSE_ADMINISTRATION_VIEW)){
            Long tenantId=scope.requireEffectiveTenant(null); LocalDateTime now=LocalDateTime.now();
            long value=licenses.countByTenantIdAndStatusInAndExpiresAtBetween(tenantId,List.of(LicenseStatus.ACTIVE,LicenseStatus.TRIAL,LicenseStatus.GRACE_PERIOD),now,now.plusDays(30));
            metrics.put("licensesNearingExpiry",metric(value,value>0?"WARNING":"NONE",value>0?"LICENSE_EXPIRY_NEAR":null,"/license"));
        }
        if(has(authentication,BootstrapAdministrationPrivileges.DATABASE_BACKUP_VIEW)){
            long running=backups.countByStatusIn(List.of(BackupJobStatus.QUEUED,BackupJobStatus.RUNNING));
            long failed=backups.countByStatusIn(List.of(BackupJobStatus.FAILED,BackupJobStatus.PARTIAL));
            metrics.put("runningBackups",metric(running,"NONE",null,"/backup"));
            metrics.put("failedBackups",metric(failed,failed>0?"CRITICAL":"NONE",failed>0?"BACKUP_FAILURE_REQUIRES_REVIEW":null,"/backup"));
        }
        if(has(authentication,BootstrapAdministrationPrivileges.API_REGISTRY_VIEW)){
            ApiRegistrySyncReportDto preview=apiService.previewSyncFromAnnotations(); long value=preview.getConflicted();
            metrics.put("apiMetadataConflicts",metric(value,value>0?"CRITICAL":"NONE",value>0?"API_METADATA_CONFLICT":null,"/access-control/api-registry"));
            diagnostics=new DashboardDiagnosticsDto(accessControl.getEnforcementMode().name(),accessControl.isEnabled(),apis.findLatestSynchronizationAt());
        }
        return new DashboardAggregateDto(LocalDateTime.now(),Collections.unmodifiableMap(metrics),diagnostics);
    }
    private boolean has(Authentication authentication,String privilege){return privileges.has(authentication,privilege);}
    private DashboardMetricDto metric(long value,String severity,String warning,String route){return new DashboardMetricDto(value,severity,warning,route);}
}
