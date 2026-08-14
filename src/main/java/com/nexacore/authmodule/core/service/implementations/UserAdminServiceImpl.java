package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.RoleDto;
import com.nexacore.authmodule.core.dto.RoleRequestDto;
import com.nexacore.authmodule.core.dto.UserDto;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.dto.UserRoleAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentRequestDto;
import com.nexacore.authmodule.core.entity.AuthUserScopeAssignment;
import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.authmodule.core.service.interfaces.UserAdminService;
import com.nexacore.authmodule.core.service.UsernameNormalizer;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.nexacore.commonmodule.dto.VersionedAssignmentDto;
import com.nexacore.commonmodule.util.AssignmentVersion;
import com.nexacore.systemmodule.tenant.service.AuthorizedScopeLookupService;
import com.nexacore.gatewaymodule.tenant.service.interfaces.TenantProvisioningGateway;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "authTransactionManager")
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonModuleGateway personModuleGateway;
    private final PasswordEncoder passwordEncoder;
    private final UsernameNormalizer usernameNormalizer;
    private final DataScopeService dataScopeService;
    private final AuthorizedScopeLookupService scopeLookupService;
    private final TenantProvisioningGateway tenantProvisioningGateway;

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public List<UserDto> listUsers(Long requestedTenantId) {
        Long tenantId = resolveAdministrationTenant(requestedTenantId);
        return userRepository.findByTenantIdOrderByNormalizedUsernameAsc(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public UserDto getUser(Long userId) {
        return toDto(requireScopedUser(userId));
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public VersionedAssignmentDto<RoleDto> getUserRoleAssignments(Long userId) {
        List<RoleDto> roles = requireScopedUser(userId).getRoles().stream()
                .sorted(java.util.Comparator.comparing(AuthRole::getId)).map(RoleDto::fromEntity).toList();
        return new VersionedAssignmentDto<>(AssignmentVersion.of(roles.stream().map(RoleDto::getId).toList()), roles);
    }

    @Override
    public UserDto saveUser(UserRequestDto requestDto) {
        if (requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }

        String normalizedUsername = usernameNormalizer.normalize(requestDto.getUsername());

        AuthUser user;
        if (requestDto.getId() == null) {
            Long tenantId = resolveCreationTenant(requestDto.getTenantId());
            userRepository.findByTenantIdAndNormalizedUsername(tenantId, normalizedUsername)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Username already in use for the tenant: " + requestDto.getUsername());
                    });
            if (requestDto.getPassword() == null || requestDto.getPassword().isBlank()) {
                throw new IllegalArgumentException("password is required when creating a user");
            }
            PersonSummaryDto person = personModuleGateway.ensurePersonForUser(
                    requestDto.getUsername(),
                    requestDto.getEmail(),
                    requestDto.getMobile(),
                    requestDto.getFirstName(),
                    requestDto.getLastName()
            );
            user = new AuthUser();
            user.setPersonId(person.getId());
            user.setTenantId(tenantId);
            user.setUsername(requestDto.getUsername().trim());
            user.setNormalizedUsername(normalizedUsername);
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
            long actorId = AuthenticatedRequestContextHolder.get()
                    .map(context -> context.userId() == null ? 0L : context.userId()).orElse(0L);
            user.getScopeAssignments().add(AuthUserScopeAssignment.builder()
                    .user(user).tenantId(tenantId).active(true)
                    .createdBy(actorId).updatedBy(actorId).build());
        } else {
            Long effectiveTenantId = resolveAdministrationTenant(requestDto.getTenantId());
            user = userRepository.findByIdAndTenantId(requestDto.getId(), effectiveTenantId)
                    .orElseThrow(() -> new DataScopeAccessDeniedException("User account not found in the effective tenant"));
            userRepository.findByTenantIdAndNormalizedUsername(effectiveTenantId, normalizedUsername)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Username already in use for the tenant: "
                                + requestDto.getUsername());
                    });
            user.setNormalizedUsername(normalizedUsername);
            user.setUsername(requestDto.getUsername().trim());
            // email/mobile/firstName/lastName belong to the linked Person record and are only
            // ever set at creation time (via ensurePersonForUser) — this form doesn't edit an
            // existing Person; that's the KYC Person module's job.
            if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
            }
        }

        user.setEnabled(requestDto.isEnabled());
        return toDto(userRepository.save(user));
    }

    private Long resolveCreationTenant(Long requestedTenantId) {
        return resolveAdministrationTenant(requestedTenantId);
    }

    private Long resolveAdministrationTenant(Long requestedTenantId) {
        if (requestedTenantId == null) {
            return dataScopeService.requireEffectiveTenant(null);
        }
        try {
            return dataScopeService.requireEffectiveTenant(requestedTenantId);
        } catch (DataScopeAccessDeniedException exception) {
            if (!isPlatformAdministrator()) throw exception;
            tenantProvisioningGateway.requireActiveTenant(requestedTenantId);
            return requestedTenantId;
        }
    }

    private boolean isPlatformAdministrator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_SYSTEM_ADMIN".equals(authority.getAuthority()));
    }

    @Override
    public void assignRoles(UserRoleAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        AuthUser user = requireScopedUser(requestDto.getUserId());

        AssignmentVersion.requireCurrent(requestDto.getVersion(),
                user.getRoles().stream().map(AuthRole::getId).toList());

        Set<AuthRole> roles = new HashSet<>(roleRepository.findAllById(requestDto.getRoleIds()));
        if (roles.size() != requestDto.getRoleIds().size()) {
            throw new IllegalArgumentException("One or more role ids were not found");
        }
        roles.forEach(role -> {
            if (!role.isActive()) {
                throw new IllegalArgumentException("Inactive role cannot be assigned: " + role.getName());
            }
            if (role.getTenantId() != null && !role.getTenantId().equals(user.getTenantId())) {
                throw new DataScopeAccessDeniedException(
                        "Tenant role cannot be assigned to an account in another tenant");
            }
        });

        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public VersionedAssignmentDto<UserScopeAssignmentDto> getScopeAssignments(Long userId) {
        AuthUser user = requireScopedUser(userId);
        List<UserScopeAssignmentDto> items = user.getScopeAssignments().stream()
                .map(scope -> new UserScopeAssignmentDto(
                        scope.getTenantId(), scope.getBusinessId(), scope.getBranchId(), scope.isActive()))
                .sorted(java.util.Comparator.comparing(scope -> scopeKey(scope.tenantId(),scope.businessId(),scope.branchId())))
                .toList();
        return new VersionedAssignmentDto<>(AssignmentVersion.of(items.stream()
                .map(scope -> scopeKey(scope.tenantId(),scope.businessId(),scope.branchId())).toList()),items);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager")
    public void replaceScopeAssignments(UserScopeAssignmentRequestDto requestDto) {
        AuthUser user = requireScopedUser(requestDto.getUserId());
        Set<UserScopeAssignmentDto> requested = requestDto.getScopeAssignments() == null
                ? Set.of() : Set.copyOf(requestDto.getScopeAssignments());
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("At least one active scope assignment is required");
        }
        if (requested.stream().anyMatch(scope -> !scope.active() || !user.getTenantId().equals(scope.tenantId()))) {
            throw new DataScopeAccessDeniedException("User scopes must be active and belong to the account tenant");
        }
        AssignmentVersion.requireCurrent(requestDto.getVersion(),user.getScopeAssignments().stream()
                .map(scope -> scopeKey(scope.getTenantId(),scope.getBusinessId(),scope.getBranchId())).toList());
        Set<String> requestedKeys = requested.stream().map(scope -> scopeKey(scope.tenantId(),scope.businessId(),scope.branchId()))
                .collect(java.util.stream.Collectors.toSet());
        user.getScopeAssignments().forEach(scope -> {
            if (!scopeLookupService.canManageAssignment(scope.getTenantId(),scope.getBusinessId(),scope.getBranchId())
                    && !requestedKeys.contains(scopeKey(scope.getTenantId(),scope.getBusinessId(),scope.getBranchId()))) {
                throw new DataScopeAccessDeniedException("Assignments outside the effective scope are locked and cannot be removed");
            }
        });
        Set<String> currentKeys = user.getScopeAssignments().stream()
                .map(scope -> scopeKey(scope.getTenantId(),scope.getBusinessId(),scope.getBranchId()))
                .collect(java.util.stream.Collectors.toSet());
        requested.stream().filter(scope -> !currentKeys.contains(scopeKey(scope.tenantId(),scope.businessId(),scope.branchId())))
                .forEach(scope -> scopeLookupService.validateAssignment(scope.tenantId(),scope.businessId(),scope.branchId()));
        long actorId = AuthenticatedRequestContextHolder.get()
                .map(context -> context.userId() == null ? 0L : context.userId()).orElse(0L);
        user.getScopeAssignments().clear();
        requested.forEach(scope -> user.getScopeAssignments().add(AuthUserScopeAssignment.builder()
                .user(user)
                .userId(user.getId())
                .tenantId(scope.tenantId())
                .businessId(scope.businessId())
                .branchId(scope.branchId())
                .active(true)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build()));
        userRepository.save(user);
    }

    private AuthUser requireScopedUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        var scoped = userRepository.findByIdAndTenantId(userId, tenantId);
        if (scoped.isPresent()) return scoped.get();
        if (isPlatformAdministrator()) {
            AuthUser target = userRepository.findById(userId)
                    .orElseThrow(() -> new DataScopeAccessDeniedException("User account not found"));
            tenantProvisioningGateway.requireActiveTenant(target.getTenantId());
            return target;
        }
        throw new DataScopeAccessDeniedException("User account not found in the effective tenant");
    }

    private String scopeKey(Long tenantId,Long businessId,Long branchId) {
        return tenantId+":"+(businessId==null?"":businessId)+":"+(branchId==null?"":branchId);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public List<RoleDto> listRoles() {
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        return roleRepository.findByTenantIdIsNullOrTenantIdOrderByNameAsc(tenantId).stream()
                .map(RoleDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public RoleDto getRole(Long roleId) {
        if (roleId == null) throw new IllegalArgumentException("roleId is required");
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        AuthRole role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .or(() -> roleRepository.findByIdAndTenantIdIsNull(roleId))
                .orElseThrow(() -> new DataScopeAccessDeniedException("Role not found in the effective tenant"));
        return RoleDto.fromEntity(role);
    }

    @Override
    public RoleDto saveRole(RoleRequestDto requestDto) {
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        return saveRole(requestDto, tenantId, false);
    }

    @Override
    public RoleDto saveGlobalRole(RoleRequestDto requestDto) {
        return saveRole(requestDto, null, true);
    }

    private RoleDto saveRole(RoleRequestDto requestDto, Long tenantId, boolean globalTemplate) {
        if (requestDto.getName() == null || requestDto.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (requestDto.getRoleCode() == null || requestDto.getRoleCode().isBlank()) {
            throw new IllegalArgumentException("roleCode is required");
        }

        var sameName = globalTemplate
                ? roleRepository.findByTenantIdIsNullAndNameIgnoreCase(requestDto.getName().trim())
                : roleRepository.findByTenantIdAndNameIgnoreCase(tenantId, requestDto.getName().trim());
        sameName.ifPresent(existing -> {
            if (!existing.getId().equals(requestDto.getId())) {
                throw new IllegalArgumentException("Role name already in use in this role scope: " + requestDto.getName());
            }
        });

        String roleCode = requestDto.getRoleCode().trim().toUpperCase(java.util.Locale.ROOT);
        var sameCode = globalTemplate
                ? roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase(roleCode)
                : roleRepository.findByTenantIdAndRoleCodeIgnoreCase(tenantId, roleCode);
        sameCode.ifPresent(existing -> {
            if (!existing.getId().equals(requestDto.getId())) {
                throw new IllegalArgumentException("Role code already in use in this role scope: " + roleCode);
            }
        });

        long actor = AuthenticatedRequestContextHolder.get()
                .map(context -> context.userId() == null ? 0L : context.userId()).orElse(0L);
        AuthRole role;
        if (requestDto.getId() == null) {
            role = new AuthRole();
            role.setTenantId(tenantId);
            role.setRoleCode(roleCode);
            role.setCreatedBy(actor);
        } else {
            role = globalTemplate
                    ? roleRepository.findByIdAndTenantIdIsNull(requestDto.getId())
                        .orElseThrow(() -> new DataScopeAccessDeniedException("Global role template not found"))
                    : roleRepository.findByIdAndTenantId(requestDto.getId(), tenantId)
                        .orElseThrow(() -> new DataScopeAccessDeniedException("Tenant role not found in the effective tenant"));
            if (!roleCode.equalsIgnoreCase(role.getRoleCode())) {
                throw new IllegalArgumentException("roleCode is immutable after role creation");
            }
        }

        role.setName(requestDto.getName().trim());
        role.setDescription(requestDto.getDescription() == null ? null : requestDto.getDescription().trim());
        role.setActive(requestDto.isActive());
        role.setUpdatedBy(actor);
        return RoleDto.fromEntity(roleRepository.save(role));
    }

    private UserDto toDto(AuthUser user) {
        PersonSummaryDto person = personModuleGateway.findSummaryById(user.getPersonId()).orElse(null);
        String personName = person == null ? null : (nullToEmpty(person.getFirstName()) + " " + nullToEmpty(person.getLastName())).trim();

        return UserDto.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .normalizedUsername(user.getNormalizedUsername())
                .personId(user.getPersonId())
                .personName(personName == null || personName.isBlank() ? null : personName)
                .email(person == null ? null : person.getEmail())
                .mobile(person == null ? null : person.getMobileNumber())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(RoleDto::fromEntity).toList())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
