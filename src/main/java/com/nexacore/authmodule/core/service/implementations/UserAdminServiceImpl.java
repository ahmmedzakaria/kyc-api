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

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public List<UserDto> listUsers() {
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
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
            Long tenantId = dataScopeService.requireEffectiveTenant(requestDto.getTenantId());
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
        } else {
            Long effectiveTenantId = dataScopeService.requireEffectiveTenant(requestDto.getTenantId());
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

    @Override
    public void assignRoles(UserRoleAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        AuthUser user = userRepository.findByIdAndTenantId(requestDto.getUserId(), tenantId)
                .orElseThrow(() -> new DataScopeAccessDeniedException("User account not found in the effective tenant"));

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
    public Set<UserScopeAssignmentDto> getScopeAssignments(Long userId) {
        AuthUser user = requireScopedUser(userId);
        return user.getScopeAssignments().stream()
                .map(scope -> new UserScopeAssignmentDto(
                        scope.getTenantId(), scope.getBusinessId(), scope.getBranchId(), scope.isActive()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
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
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new DataScopeAccessDeniedException("User account not found in the effective tenant"));
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
