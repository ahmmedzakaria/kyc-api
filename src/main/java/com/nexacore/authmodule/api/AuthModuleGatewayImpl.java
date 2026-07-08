package com.nexacore.authmodule.api;

import com.nexacore.authmodule.core.entity.Role;
import com.nexacore.authmodule.core.entity.User;
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return AuthUserAccessDto.builder()
                .userId(user.getId())
                .roleIds(user.getRoles().stream()
                        .map(Role::getId)
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
