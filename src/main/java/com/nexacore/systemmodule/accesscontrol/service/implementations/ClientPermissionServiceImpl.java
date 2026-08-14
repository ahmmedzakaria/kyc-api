package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientAdministrationDetailDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientScopeAssignmentDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApiPermission;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplicationTenant;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientFeaturePermission;
import com.nexacore.systemmodule.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientPermissionService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.commonmodule.util.AssignmentVersion;
import com.nexacore.systemmodule.tenant.service.AuthorizedScopeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;

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
    private final AuthorizationDataCache authorizationDataCache;
    private final AuthorizedScopeLookupService scopeLookupService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysAccClientApplication application = resolveClient(requestDto);
        Set<Long> apiRegistryIds = requestDto.getApiRegistryIds() == null ? Set.of() : requestDto.getApiRegistryIds();
        Set<Long> existingApiRegistryIds = clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(application.getId());
        if (existingApiRegistryIds.equals(apiRegistryIds)) {
            return;
        }

        clientApiPermissionRepository.deleteByClientApplicationId(application.getId());
        clientApiPermissionRepository.saveAll(apiRegistryIds.stream()
                .map(apiId -> {
                    SysAccApiRegistry api = apiRegistryRepository.findById(apiId)
                            .orElseThrow(() -> new IllegalArgumentException("API registry not found: " + apiId));
                    return SysAccClientApiPermission.builder()
                            .clientApplication(application)
                            .apiRegistry(api)
                            .active(true)
                            .createdBy(actorId)
                            .updatedBy(actorId)
                            .build();
                })
                .toList());
        authorizationDataCache.invalidateClientAfterCommit(application.getId());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void grantApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        SysAccClientApplication application = resolveClient(requestDto);
        Set<Long> mergedApiRegistryIds = new HashSet<>(
                clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(application.getId()));
        if (requestDto.getApiRegistryIds() != null) {
            mergedApiRegistryIds.addAll(requestDto.getApiRegistryIds());
        }

        ClientPermissionAssignmentRequestDto mergedRequest = new ClientPermissionAssignmentRequestDto();
        mergedRequest.setClientApplicationId(application.getId());
        mergedRequest.setApiRegistryIds(mergedApiRegistryIds);
        assignApiPermissions(mergedRequest, username);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysAccClientApplication application = resolveClient(requestDto);
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
                    return SysAccClientFeaturePermission.builder()
                            .clientApplication(application)
                            .privilege(privilege)
                            .active(true)
                            .createdBy(actorId)
                            .updatedBy(actorId)
                            .build();
                })
                .toList());
        authorizationDataCache.invalidateClientAfterCommit(application.getId());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void grantFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username) {
        SysAccClientApplication application = resolveClient(requestDto);
        Set<String> mergedPrivilegeCodes = new HashSet<>(
                clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(application.getId()));
        if (requestDto.getPrivilegeCodes() != null) {
            mergedPrivilegeCodes.addAll(requestDto.getPrivilegeCodes());
        }

        ClientPermissionAssignmentRequestDto mergedRequest = new ClientPermissionAssignmentRequestDto();
        mergedRequest.setClientApplicationId(application.getId());
        mergedRequest.setPrivilegeCodes(mergedPrivilegeCodes);
        assignFeaturePermissions(mergedRequest, username);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignTenants(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysAccClientApplication application = resolveClient(requestDto);
        Set<ClientScopeAssignmentDto> assignments = normalizedScopes(requestDto);
        Set<ClientScopeAssignmentDto> current = getScopeAssignments(application.getId(), null);
        if (requestDto.getVersion()!=null) AssignmentVersion.requireCurrent(requestDto.getVersion(),current.stream().map(this::scopeKey).toList());
        Set<String> requestedKeys=assignments.stream().map(this::scopeKey).collect(java.util.stream.Collectors.toSet());
        current.forEach(scope -> {
            if (!scopeLookupService.canManageAssignment(scope.tenantId(),scope.businessId(),scope.branchId())
                    && !requestedKeys.contains(scopeKey(scope))) {
                throw new DataScopeAccessDeniedException("Assignments outside the effective scope are locked and cannot be removed");
            }
        });
        Set<String> currentKeys=current.stream().map(this::scopeKey).collect(java.util.stream.Collectors.toSet());
        assignments.stream().filter(scope -> !currentKeys.contains(scopeKey(scope)))
                .forEach(scope -> scopeLookupService.validateAssignment(scope.tenantId(),scope.businessId(),scope.branchId()));
        if (current.equals(assignments)) return;
        clientApplicationTenantRepository.deleteByClientApplicationId(application.getId());
        clientApplicationTenantRepository.saveAll(assignments.stream()
                .map(scope -> SysAccClientApplicationTenant.builder()
                        .clientApplication(application)
                        .tenantId(scope.tenantId())
                        .businessId(scope.businessId())
                        .branchId(scope.branchId())
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList());
        authorizationDataCache.invalidateClientAfterCommit(application.getId());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void grantTenants(ClientPermissionAssignmentRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysAccClientApplication application = resolveClient(requestDto);
        Set<ClientScopeAssignmentDto> requested = normalizedScopes(requestDto);
        if (requested.stream().anyMatch(scope -> scope.businessId() != null || scope.branchId() != null)) {
            throw new IllegalArgumentException("Bootstrap tenant grants must be tenant-level assignments");
        }
        Set<String> currentKeys = getScopeAssignments(application.getId(), null).stream()
                .map(this::scopeKey).collect(java.util.stream.Collectors.toSet());
        var additions = requested.stream().filter(scope -> !currentKeys.contains(scopeKey(scope)))
                .map(scope -> SysAccClientApplicationTenant.builder()
                        .clientApplication(application)
                        .tenantId(scope.tenantId())
                        .active(true)
                        .createdBy(actorId)
                        .updatedBy(actorId)
                        .build())
                .toList();
        if (additions.isEmpty()) return;
        clientApplicationTenantRepository.saveAll(additions);
        authorizationDataCache.invalidateClientAfterCommit(application.getId());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ClientAdministrationDetailDto getAdministrationDetail(Long clientApplicationId, String clientCode) {
        SysAccClientApplication application = clientApplicationService.requireClientApplication(clientApplicationId, clientCode);
        return ClientAdministrationDetailDto.builder()
                .client(com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationDto.fromEntity(application))
                .apiRegistryIds(getApiPermissions(application.getId(), null))
                .privilegeCodes(getFeaturePermissions(application.getId(), null))
                .scopeAssignments(getScopeAssignments(application.getId(), null))
                .scopeVersion(AssignmentVersion.of(getScopeAssignments(application.getId(), null).stream().map(this::scopeKey).toList()))
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<Long> getApiPermissions(Long clientApplicationId, String clientCode) {
        SysAccClientApplication application = clientApplicationService.requireClientApplication(clientApplicationId, clientCode);
        return Set.copyOf(clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(application.getId()));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<String> getFeaturePermissions(Long clientApplicationId, String clientCode) {
        SysAccClientApplication application = clientApplicationService.requireClientApplication(clientApplicationId, clientCode);
        return Set.copyOf(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(application.getId()));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<ClientScopeAssignmentDto> getScopeAssignments(Long clientApplicationId, String clientCode) {
        SysAccClientApplication application = clientApplicationService.requireClientApplication(clientApplicationId, clientCode);
        return clientApplicationTenantRepository.findByClientApplicationIdAndActiveTrue(application.getId()).stream()
                .map(scope -> new ClientScopeAssignmentDto(scope.getTenantId(), scope.getBusinessId(), scope.getBranchId()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<ClientScopeAssignmentDto> normalizedScopes(ClientPermissionAssignmentRequestDto requestDto) {
        if (requestDto.getScopeAssignments() != null) return Set.copyOf(requestDto.getScopeAssignments());
        if (requestDto.getBusinessIds() != null && !requestDto.getBusinessIds().isEmpty()) {
            throw new IllegalArgumentException("scopeAssignments must be used for business or branch scope replacement");
        }
        return requestDto.getTenantIds() == null ? Set.of() : requestDto.getTenantIds().stream()
                .map(tenantId -> new ClientScopeAssignmentDto(tenantId, null, null))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private SysAccClientApplication resolveClient(ClientPermissionAssignmentRequestDto requestDto) {
        return clientApplicationService.requireClientApplication(requestDto.getClientApplicationId(), requestDto.getClientCode());
    }
    private String scopeKey(ClientScopeAssignmentDto scope) {
        return scope.tenantId()+":"+(scope.businessId()==null?"":scope.businessId())+":"+(scope.branchId()==null?"":scope.branchId());
    }
}
