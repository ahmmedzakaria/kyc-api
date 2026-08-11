package com.nexacore.authmodule.api;

import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.dto.AuthUserScopeAssignmentDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import com.nexacore.authmodule.security.service.TenantAccountUserDetails;

@Component
@RequiredArgsConstructor
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class AuthModuleGatewayImpl implements AuthModuleGateway {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public AuthUserAccessDto getUserAccess(String username) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TenantAccountUserDetails principal)
                || !principal.getUsername().equals(username)) {
            throw new IllegalArgumentException("Tenant-bound authenticated account is required");
        }
        return getUserAccess(principal.accountId(), principal.tenantId());
    }

    @Override
    public AuthUserAccessDto getUserAccess(Long accountId, Long tenantId) {
        AuthUser user = userRepository.findByIdAndTenantId(accountId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant account not found"));
        return AuthUserAccessDto.builder()
                .userId(user.getId())
                .personId(user.getPersonId())
                .tenantId(user.getTenantId())
                .scopeAssignments(user.getScopeAssignments().stream()
                        .filter(com.nexacore.authmodule.core.entity.AuthUserScopeAssignment::isActive)
                        .map(scope -> new AuthUserScopeAssignmentDto(
                                scope.getTenantId(), scope.getBusinessId(), scope.getBranchId()))
                        .collect(Collectors.toUnmodifiableSet()))
                .roleIds(user.getRoles().stream()
                        .map(AuthRole::getId)
                        .collect(Collectors.toSet()))
                .admin(user.getRoles().stream().anyMatch(role -> ADMIN_ROLE.equals(role.getName())))
                .build();
    }

    @Override
    public Long getUserId(String username) {
        return getUserAccess(username).userId();
    }

    @Override
    public void requireUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }

    @Override
    public void requireRoleExists(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }
    }
}
