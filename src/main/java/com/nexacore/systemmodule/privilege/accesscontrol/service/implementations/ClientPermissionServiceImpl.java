package com.nexacore.systemmodule.privilege.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApiPermission;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplicationTenant;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientFeaturePermission;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientPermissionService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientPermissionServiceImpl implements ClientPermissionService {

    private final ClientApplicationService clientApplicationService;
    private final ApiRegistryRepository apiRegistryRepository;
    private final PrivilegeRepository privilegeRepository;
    private final ClientApiPermissionRepository clientApiPermissionRepository;
    private final ClientFeaturePermissionRepository clientFeaturePermissionRepository;
    private final ClientApplicationTenantRepository clientApplicationTenantRepository;
    private final AuthModuleGateway authModuleGateway;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysClientApplication application = resolveClient(requestDto);
        Set<Long> apiRegistryIds = requestDto.getApiRegistryIds() == null ? Set.of() : requestDto.getApiRegistryIds();
        Set<Long> existingApiRegistryIds = clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(application.getId());
        if (existingApiRegistryIds.equals(apiRegistryIds)) {
            return;
        }

        clientApiPermissionRepository.deleteByClientApplicationId(application.getId());
        clientApiPermissionRepository.saveAll(apiRegistryIds.stream()
                .map(apiId -> {
                    SysApiRegistry api = apiRegistryRepository.findById(apiId)
                            .orElseThrow(() -> new IllegalArgumentException("API registry not found: " + apiId));
                    return SysClientApiPermission.builder()
                            .clientApplication(application)
                            .apiRegistry(api)
                            .active(true)
                            .createdBy(actorId)
                            .updatedBy(actorId)
                            .build();
                })
                .toList());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysClientApplication application = resolveClient(requestDto);
        Set<String> privilegeCodes = requestDto.getPrivilegeCodes() == null ? Set.of() : requestDto.getPrivilegeCodes();
        Set<String> existingPrivilegeCodes = clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(application.getId());
        if (existingPrivilegeCodes.equals(privilegeCodes)) {
            return;
        }

        clientFeaturePermissionRepository.deleteByClientApplicationId(application.getId());
        clientFeaturePermissionRepository.saveAll(privilegeCodes.stream()
                .map(code -> {
                    SysPrivilege privilege = privilegeRepository.findByPrivilegeCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Privilege code not found: " + code));
                    return SysClientFeaturePermission.builder()
                            .clientApplication(application)
                            .privilege(privilege)
                            .active(true)
                            .createdBy(actorId)
                            .updatedBy(actorId)
                            .build();
                })
                .toList());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignTenants(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysClientApplication application = resolveClient(requestDto);
        clientApplicationTenantRepository.deleteByClientApplicationId(application.getId());
        Set<Long> tenantIds = requestDto.getTenantIds() == null ? Set.of() : requestDto.getTenantIds();
        Set<Long> businessIds = requestDto.getBusinessIds() == null ? Set.of() : requestDto.getBusinessIds();

        clientApplicationTenantRepository.saveAll(tenantIds.stream()
                .map(tenantId -> SysClientApplicationTenant.builder()
                        .clientApplication(application)
                        .tenantId(tenantId)
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList());
        clientApplicationTenantRepository.saveAll(businessIds.stream()
                .map(businessId -> SysClientApplicationTenant.builder()
                        .clientApplication(application)
                        .businessId(businessId)
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList());
    }

    private SysClientApplication resolveClient(ClientPermissionAssignmentRequestDto requestDto) {
        return clientApplicationService.requireClientApplication(requestDto.getClientApplicationId(), requestDto.getClientCode());
    }
}
