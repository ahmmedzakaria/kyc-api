package com.example.kyc.authmodule.service.implementations;

import com.example.kyc.authmodule.dto.PrivilegeAssignmentRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckResponseDto;
import com.example.kyc.authmodule.dto.PrivilegeDto;
import com.example.kyc.authmodule.dto.PrivilegeRequestDto;
import com.example.kyc.authmodule.entity.Privilege;
import com.example.kyc.authmodule.entity.Role;
import com.example.kyc.authmodule.entity.User;
import com.example.kyc.authmodule.repository.PrivilegeRepository;
import com.example.kyc.authmodule.repository.RoleRepository;
import com.example.kyc.authmodule.repository.UserRepository;
import com.example.kyc.authmodule.service.interfaces.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

    private static final int MODULE_CODE_LENGTH = 2;
    private static final int FEATURE_TYPE_CODE_LENGTH = 2;
    private static final int FEATURE_CODE_LENGTH = 3;
    private static final int ACTION_CODE_LENGTH = 2;

    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public String buildPrivilegeCode(String moduleCode, String featureTypeCode, String featureCode, String actionCode) {
        validateCodePart(moduleCode, MODULE_CODE_LENGTH, "moduleCode");
        validateCodePart(featureTypeCode, FEATURE_TYPE_CODE_LENGTH, "featureTypeCode");
        validateCodePart(featureCode, FEATURE_CODE_LENGTH, "featureCode");
        validateCodePart(actionCode, ACTION_CODE_LENGTH, "actionCode");

        return moduleCode + featureTypeCode + featureCode + actionCode;
    }

    @Override
    @Transactional
    public PrivilegeDto savePrivilege(PrivilegeRequestDto requestDto) {
        String privilegeCode = buildPrivilegeCode(
                requestDto.getModuleCode(),
                requestDto.getFeatureTypeCode(),
                requestDto.getFeatureCode(),
                requestDto.getActionCode()
        );

        Privilege privilege = privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(Privilege::new);

        privilege.setPrivilegeCode(privilegeCode);
        privilege.setModuleCode(requestDto.getModuleCode());
        privilege.setModuleName(requestDto.getModuleName());
        privilege.setFeatureTypeCode(requestDto.getFeatureTypeCode());
        privilege.setFeatureTypeName(requestDto.getFeatureTypeName());
        privilege.setFeatureCode(requestDto.getFeatureCode());
        privilege.setFeatureName(requestDto.getFeatureName());
        privilege.setActionCode(requestDto.getActionCode());
        privilege.setActionName(requestDto.getActionName());
        privilege.setActive(requestDto.getActive() == null || requestDto.getActive());

        return PrivilegeDto.fromEntity(privilegeRepository.save(privilege));
    }

    @Override
    public List<PrivilegeDto> getAllPrivileges() {
        return privilegeRepository.findAll().stream()
                .map(PrivilegeDto::fromEntity)
                .toList();
    }

    @Override
    public Set<String> getUserPrivilegeCodes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Set<String> directPrivileges = user.getPrivileges().stream()
                .filter(Privilege::isActive)
                .map(Privilege::getPrivilegeCode)
                .collect(Collectors.toSet());

        Set<String> rolePrivileges = user.getRoles().stream()
                .flatMap(role -> role.getPrivileges().stream())
                .filter(Privilege::isActive)
                .map(Privilege::getPrivilegeCode)
                .collect(Collectors.toSet());

        return Stream.concat(directPrivileges.stream(), rolePrivileges.stream())
                .collect(Collectors.toSet());
    }

    @Override
    public PrivilegeCheckResponseDto checkPrivilege(PrivilegeCheckRequestDto requestDto) {
        String privilegeCode = requestDto.getPrivilegeCode();
        if (privilegeCode == null || privilegeCode.isBlank()) {
            privilegeCode = buildPrivilegeCode(
                    requestDto.getModuleCode(),
                    requestDto.getFeatureTypeCode(),
                    requestDto.getFeatureCode(),
                    requestDto.getActionCode()
            );
        }

        boolean allowed = getUserPrivilegeCodes(requestDto.getUsername()).contains(privilegeCode);

        return PrivilegeCheckResponseDto.builder()
                .username(requestDto.getUsername())
                .privilegeCode(privilegeCode)
                .allowed(allowed)
                .build();
    }

    @Override
    @Transactional
    public void assignPrivilegesToRole(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getRoleId() == null) {
            throw new IllegalArgumentException("roleId is required");
        }

        Role role = roleRepository.findById(requestDto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + requestDto.getRoleId()));

        role.setPrivileges(loadPrivileges(requestDto.getPrivilegeCodes()));
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestDto.getUserId()));

        user.setPrivileges(loadPrivileges(requestDto.getPrivilegeCodes()));
        userRepository.save(user);
    }

    private Set<Privilege> loadPrivileges(Set<String> privilegeCodes) {
        if (privilegeCodes == null || privilegeCodes.isEmpty()) {
            return new HashSet<>();
        }

        List<Privilege> privileges = privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
        if (privileges.size() != privilegeCodes.size()) {
            Set<String> foundCodes = privileges.stream()
                    .map(Privilege::getPrivilegeCode)
                    .collect(Collectors.toSet());

            Set<String> missingCodes = privilegeCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.toSet());

            throw new IllegalArgumentException("Privilege code not found: " + missingCodes);
        }

        return new HashSet<>(privileges);
    }

    private void validateCodePart(String value, int expectedLength, String fieldName) {
        if (value == null || !value.matches("\\d{" + expectedLength + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + expectedLength + " digits");
        }
    }
}
