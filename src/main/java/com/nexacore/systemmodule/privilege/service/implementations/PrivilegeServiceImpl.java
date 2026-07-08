package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.PrivilegeAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckResponseDto;
import com.nexacore.authmodule.core.dto.PrivilegeDto;
import com.nexacore.authmodule.core.dto.PrivilegeRequestDto;
import com.nexacore.authmodule.core.dto.SidebarMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuRequestDto;
import com.nexacore.authmodule.core.entity.Role;
import com.nexacore.authmodule.core.entity.User;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.systemmodule.privilege.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.entity.Privilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilegeId;
import com.nexacore.systemmodule.privilege.entity.SubMenu;
import com.nexacore.systemmodule.privilege.entity.UserPrivilege;
import com.nexacore.systemmodule.privilege.entity.UserPrivilegeId;
import com.nexacore.systemmodule.privilege.enums.FeatureType;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.repository.UserPrivilegeRepository;
import com.nexacore.systemmodule.privilege.service.interfaces.ModulePrivilegeProvider;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

    private static final int MODULE_CODE_LENGTH = 2;
    private static final int SUBMODULE_CODE_LENGTH = 2;
    private static final int FEATURE_TYPE_CODE_LENGTH = 2;
    private static final int FEATURE_CODE_LENGTH = 3;
    private static final int ACTION_CODE_LENGTH = 2;

    private final PrivilegeRepository privilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final UserPrivilegeRepository userPrivilegeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SubMenuRepository subMenuRepository;
    private final List<ModulePrivilegeProvider> modulePrivilegeProviders;

    @Override
    public String buildPrivilegeCode(String moduleCode, String submoduleCode, String featureTypeCode, String featureCode, String actionCode) {
        validateCodePart(moduleCode, MODULE_CODE_LENGTH, "moduleCode");
        validateCodePart(submoduleCode, SUBMODULE_CODE_LENGTH, "submoduleCode");
        validateCodePart(featureTypeCode, FEATURE_TYPE_CODE_LENGTH, "featureTypeCode");
        validateCodePart(featureCode, FEATURE_CODE_LENGTH, "featureCode");
        validateCodePart(actionCode, ACTION_CODE_LENGTH, "actionCode");

        return moduleCode + submoduleCode + featureTypeCode + featureCode + actionCode;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public PrivilegeDto savePrivilege(PrivilegeRequestDto requestDto) {
        String privilegeCode = buildPrivilegeCode(
                requestDto.getModuleCode(),
                requestDto.getSubmoduleCode(),
                requestDto.getFeatureTypeCode(),
                requestDto.getFeatureCode(),
                requestDto.getActionCode()
        );

        Privilege privilege = privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(Privilege::new);

        privilege.setPrivilegeCode(privilegeCode);
        privilege.setModuleCode(requestDto.getModuleCode());
        privilege.setModuleName(requestDto.getModuleName());
        privilege.setSubmoduleCode(requestDto.getSubmoduleCode());
        privilege.setSubmoduleName(requestDto.getSubmoduleName());
        privilege.setFeatureTypeCode(requestDto.getFeatureTypeCode());
        privilege.setFeatureTypeName(requestDto.getFeatureTypeName());
        privilege.setFeatureCode(requestDto.getFeatureCode());
        privilege.setFeatureName(requestDto.getFeatureName());
        privilege.setActionCode(requestDto.getActionCode());
        privilege.setActionName(requestDto.getActionName());
        privilege.setSubMenu(resolveSubMenu(requestDto.getSubMenuId()));
        privilege.setActive(requestDto.getActive() == null || requestDto.getActive());

        return PrivilegeDto.fromEntity(privilegeRepository.save(privilege));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<PrivilegeDto> getAllPrivileges() {
        return privilegeRepository.findAll().stream()
                .map(PrivilegeDto::fromEntity)
                .toList();
    }

    @Override
    public List<PrivilegeFeatureDefinitionDto> getModulePrivilegeDefinitions() {
        return modulePrivilegeProviders.stream()
                .flatMap(provider -> provider.getPrivilegeFeatures().stream())
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public SubMenuDto saveSubMenu(SubMenuRequestDto requestDto, String username) {
        Long loginUserId = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getId();

        SubMenu subMenu = requestDto.getId() == null
                ? new SubMenu()
                : subMenuRepository.findById(requestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sub menu not found: " + requestDto.getId()));

        subMenu.setName(requestDto.getName());
        subMenu.setUrl(requestDto.getUrl());
        subMenu.setIcon(requestDto.getIcon());
        subMenu.setModuleCode(requestDto.getModuleCode());
        subMenu.setModuleName(requestDto.getModuleName());
        subMenu.setSubmoduleCode(requestDto.getSubmoduleCode());
        subMenu.setSubmoduleName(requestDto.getSubmoduleName());
        subMenu.setFeatureTypeCode(requestDto.getFeatureTypeCode());
        subMenu.setFeatureTypeName(requestDto.getFeatureTypeName());
        subMenu.setFeatureCode(requestDto.getFeatureCode());
        subMenu.setFeatureName(requestDto.getFeatureName());
        subMenu.setActive(requestDto.getActive() == null || requestDto.getActive());

        if (subMenu.getId() == null) {
            subMenu.setCreatedBy(loginUserId);
        }
        subMenu.setUpdatedBy(loginUserId);

        return SubMenuDto.fromEntity(subMenuRepository.save(subMenu));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SubMenuDto> getAllSubMenus() {
        return subMenuRepository.findAll().stream()
                .sorted(Comparator.comparing(SubMenu::getModuleCode)
                        .thenComparing(SubMenu::getSubmoduleCode)
                        .thenComparing(SubMenu::getFeatureTypeCode)
                        .thenComparing(SubMenu::getFeatureCode)
                        .thenComparing(SubMenu::getName))
                .map(SubMenuDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ApplicationContextDto getApplicationContext(String username) {
        return ApplicationContextDto.builder()
                .menus(getUserSidebarMenu(username))
                .privilegeCodes(getUserPrivilegeCodes(username))
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SidebarMenuDto> getUserSidebarMenu(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Set<String> userPrivilegeCodes = getUserPrivilegeCodes(username);
        boolean admin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

        return List.of(
                buildMainMenu(FeatureType.SETUP, "fa fa-sliders", userPrivilegeCodes, admin),
                buildMainMenu(FeatureType.OPERATIONS, "fa fa-briefcase", userPrivilegeCodes, admin),
                buildMainMenu(FeatureType.REPORT, "fa fa-chart-line", userPrivilegeCodes, admin)
        );
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<String> getUserPrivilegeCodes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Set<String> directPrivileges = new HashSet<>(privilegeRepository.findActivePrivilegeCodesByUserId(user.getId()));

        Set<Long> roleIds = user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        Set<String> rolePrivileges = roleIds.isEmpty()
                ? Set.of()
                : new HashSet<>(privilegeRepository.findActivePrivilegeCodesByRoleIdIn(roleIds));

        return Stream.concat(directPrivileges.stream(), rolePrivileges.stream())
                .collect(Collectors.toSet());
    }

    @Override
    public PrivilegeCheckResponseDto checkPrivilege(PrivilegeCheckRequestDto requestDto) {
        String privilegeCode = requestDto.getPrivilegeCode();
        if (privilegeCode == null || privilegeCode.isBlank()) {
            privilegeCode = buildPrivilegeCode(
                    requestDto.getModuleCode(),
                    requestDto.getSubmoduleCode(),
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
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignPrivilegesToRole(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getRoleId() == null) {
            throw new IllegalArgumentException("roleId is required");
        }

        Role role = roleRepository.findById(requestDto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + requestDto.getRoleId()));

        Set<Privilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        rolePrivilegeRepository.deleteByIdRoleId(role.getId());
        rolePrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> RolePrivilege.builder()
                        .id(new RolePrivilegeId(role.getId(), privilege.getId()))
                        .build())
                .toList());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestDto.getUserId()));

        Set<Privilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        userPrivilegeRepository.deleteByIdUserId(user.getId());
        userPrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> UserPrivilege.builder()
                        .id(new UserPrivilegeId(user.getId(), privilege.getId()))
                        .build())
                .toList());
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

    private SidebarMenuDto buildMainMenu(FeatureType featureType,
                                         String icon,
                                         Set<String> userPrivilegeCodes,
                                         boolean admin) {
        List<Privilege> privileges = admin
                ? privilegeRepository.findAll()
                : privilegeRepository.findByPrivilegeCodeIn(userPrivilegeCodes);

        Map<Long, SidebarMenuDto> childMenus = new LinkedHashMap<>();
        privileges.stream()
                .filter(Privilege::isActive)
                .filter(privilege -> privilege.getSubMenu() != null)
                .filter(privilege -> privilege.getSubMenu().isActive())
                .filter(privilege -> featureType.getCode().equals(privilege.getSubMenu().getFeatureTypeCode()))
                .sorted(Comparator.comparing((Privilege privilege) -> privilege.getSubMenu().getModuleCode())
                        .thenComparing((Privilege privilege) -> privilege.getSubMenu().getSubmoduleCode())
                        .thenComparing((Privilege privilege) -> privilege.getSubMenu().getFeatureCode())
                        .thenComparing((Privilege privilege) -> privilege.getSubMenu().getName()))
                .forEach(privilege -> childMenus.computeIfAbsent(
                        privilege.getSubMenu().getId(),
                        id -> toSidebarMenu(privilege.getSubMenu())
                ).getPrivilegeCodes().add(privilege.getPrivilegeCode()));

        List<SidebarMenuDto> featureMenus = new ArrayList<>(childMenus.values()).stream()
                .filter(menu -> admin || hasAnyPrivilege(menu.getPrivilegeCodes(), userPrivilegeCodes))
                .toList();

        return SidebarMenuDto.builder()
                .label(featureType.getDisplayName())
                .icon(icon)
                .children(featureMenus)
                .build();
    }

    private SidebarMenuDto toSidebarMenu(SubMenu subMenu) {
        return SidebarMenuDto.builder()
                .label(subMenu.getName())
                .icon(subMenu.getIcon())
                .path(subMenu.getUrl())
                .build();
    }

    private SubMenu resolveSubMenu(Long subMenuId) {
        if (subMenuId == null) {
            return null;
        }

        return subMenuRepository.findById(subMenuId)
                .orElseThrow(() -> new IllegalArgumentException("Sub menu not found: " + subMenuId));
    }

    private boolean hasAnyPrivilege(List<String> requiredPrivilegeCodes, Set<String> userPrivilegeCodes) {
        return requiredPrivilegeCodes != null
                && requiredPrivilegeCodes.stream().anyMatch(userPrivilegeCodes::contains);
    }

    private void validateCodePart(String value, int expectedLength, String fieldName) {
        if (value == null || !value.matches("\\d{" + expectedLength + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + expectedLength + " digits");
        }
    }
}
