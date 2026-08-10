package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.RoleDto;
import com.nexacore.authmodule.core.dto.RoleRequestDto;
import com.nexacore.authmodule.core.dto.UserDto;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.dto.UserRoleAssignmentRequestDto;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
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
            // Kept until Phase 5 removes the deployed global uniqueness constraint.
            userRepository.findByUsername(requestDto.getUsername()).ifPresent(existing -> {
                throw new IllegalArgumentException("Username is still reserved by a legacy global account: "
                        + requestDto.getUsername());
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
            user = userRepository.findById(requestDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestDto.getId()));
            if (user.getTenantId() != null) {
                Long effectiveTenantId = dataScopeService.requireEffectiveTenant(requestDto.getTenantId());
                if (!user.getTenantId().equals(effectiveTenantId)) {
                    throw new DataScopeAccessDeniedException("The user account belongs to another tenant");
                }
                userRepository.findByTenantIdAndNormalizedUsername(effectiveTenantId, normalizedUsername)
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Username already in use for the tenant: "
                                    + requestDto.getUsername());
                        });
                user.setNormalizedUsername(normalizedUsername);
            } else {
                userRepository.findByUsername(requestDto.getUsername())
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Username already in use: " + requestDto.getUsername());
                        });
            }
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

        AuthUser user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestDto.getUserId()));

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
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(RoleDto::fromEntity)
                .toList();
    }

    @Override
    public RoleDto saveRole(RoleRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        roleRepository.findByName(requestDto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(requestDto.getId())) {
                throw new IllegalArgumentException("Role name already in use: " + requestDto.getName());
            }
        });

        AuthRole role = requestDto.getId() == null
                ? new AuthRole()
                : roleRepository.findById(requestDto.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + requestDto.getId()));

        role.setName(requestDto.getName());
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
