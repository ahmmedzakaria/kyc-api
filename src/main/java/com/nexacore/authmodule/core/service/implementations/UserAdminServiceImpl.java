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
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        userRepository.findByUsername(requestDto.getUsername()).ifPresent(existing -> {
            if (!existing.getId().equals(requestDto.getId())) {
                throw new IllegalArgumentException("Username already in use: " + requestDto.getUsername());
            }
        });

        AuthUser user;
        if (requestDto.getId() == null) {
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
            user.setUsername(requestDto.getUsername());
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        } else {
            user = userRepository.findById(requestDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestDto.getId()));
            user.setUsername(requestDto.getUsername());
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
                .username(user.getUsername())
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
