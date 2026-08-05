package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApiPermission;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplicationTenant;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientFeaturePermission;
import com.nexacore.systemmodule.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientPermissionService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
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
        SysPrivClientApplication application = resolveClient(requestDto);
        Set<Long> apiRegistryIds = requestDto.getApiRegistryIds() == null ? Set.of() : requestDto.getApiRegistryIds();
        Set<Long> existingApiRegistryIds = clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(application.getId());
        if (existingApiRegistryIds.equals(apiRegistryIds)) {
            return;
        }

        clientApiPermissionRepository.deleteByClientApplicationId(application.getId());
        clientApiPermissionRepository.saveAll(apiRegistryIds.stream()
                .map(apiId -> {
                    SysPrivApiRegistry api = apiRegistryRepository.findById(apiId)
                            .orElseThrow(() -> new IllegalArgumentException("API registry not found: " + apiId));
                    return SysPrivClientApiPermission.builder()
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
        SysPrivClientApplication application = resolveClient(requestDto);
        Set<String> privilegeCodes = requestDto.getPrivilegeCodes() == null ? Set.of() : requestDto.getPrivilegeCodes();
        Set<String> existingPrivilegeCodes = clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(application.getId());
        if (existingPrivilegeCodes.equals(privilegeCodes)) {
            return;
        }

        clientFeaturePermissionRepository.deleteByClientApplicationId(application.getId());
        clientFeaturePermissionRepository.saveAll(privilegeCodes.stream()
                .map(code -> {
                    SysPrivPrivilege privilege = privilegeRepository.findByPrivilegeCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Privilege code not found: " + code));
                    return SysPrivClientFeaturePermission.builder()
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
        SysPrivClientApplication application = resolveClient(requestDto);
        clientApplicationTenantRepository.deleteByClientApplicationId(application.getId());
        Set<Long> tenantIds = requestDto.getTenantIds() == null ? Set.of() : requestDto.getTenantIds();
        Set<Long> businessIds = requestDto.getBusinessIds() == null ? Set.of() : requestDto.getBusinessIds();

        clientApplicationTenantRepository.saveAll(tenantIds.stream()
                .map(tenantId -> SysPrivClientApplicationTenant.builder()
                        .clientApplication(application)
                        .tenantId(tenantId)
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList());
        clientApplicationTenantRepository.saveAll(businessIds.stream()
                .map(businessId -> SysPrivClientApplicationTenant.builder()
                        .clientApplication(application)
                        .businessId(businessId)
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList());
    }

    private SysPrivClientApplication resolveClient(ClientPermissionAssignmentRequestDto requestDto) {
        return clientApplicationService.requireClientApplication(requestDto.getClientApplicationId(), requestDto.getClientCode());
    }
}
