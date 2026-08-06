package com.nexacore.authmodule.api;

import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class AuthModuleGatewayImpl implements AuthModuleGateway {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public AuthUserAccessDto getUserAccess(String username) {
        AuthUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return AuthUserAccessDto.builder()
                .userId(user.getId())
                .personId(user.getPersonId())
                .tenantId(user.getTenantId())
                .businessId(user.getBusinessId())
                .branchId(user.getBranchId())
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
