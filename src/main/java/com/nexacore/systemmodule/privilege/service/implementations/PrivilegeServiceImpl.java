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
import com.nexacore.authmodule.core.service.implementations.AuthApplicationContextService;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationContextDto;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientApplicationContextService;
import com.nexacore.systemmodule.privilege.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.entity.SysFeature;
import com.nexacore.systemmodule.privilege.entity.SysModule;
import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysRolePrivilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilegeId;
import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.entity.SysSubmodule;
import com.nexacore.systemmodule.privilege.entity.SysUserPrivilege;
import com.nexacore.systemmodule.privilege.entity.UserPrivilegeId;
import com.nexacore.systemmodule.privilege.enums.FeatureType;
import com.nexacore.systemmodule.privilege.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.repository.SubmoduleRepository;
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
import java.util.Objects;
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
    private final ModuleRepository moduleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final FeatureRepository featureRepository;
    private final AuthModuleGateway authModuleGateway;
    private final SubMenuRepository subMenuRepository;
    private final List<ModulePrivilegeProvider> modulePrivilegeProviders;
    private final ClientApplicationContextService clientApplicationContextService;
    private final AuthApplicationContextService authApplicationContextService;

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

        SysPrivilege privilege = privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(SysPrivilege::new);

        privilege.setPrivilegeCode(privilegeCode);
        privilege.setFeature(resolveFeature(requestDto));
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
        Long loginUserId = authModuleGateway.getUserId(username);

        SysSubMenu subMenu = requestDto.getId() == null
                ? new SysSubMenu()
                : subMenuRepository.findById(requestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sub menu not found: " + requestDto.getId()));

        subMenu.setName(requestDto.getName());
        subMenu.setUrl(requestDto.getUrl());
        subMenu.setIcon(requestDto.getIcon());
        subMenu.setFeature(resolveFeature(requestDto));
        subMenu.setActive(requestDto.getActive() == null || requestDto.getActive());
        subMenu.setMenuOrder(defaultOrder(requestDto.getMenuOrder()));
        subMenu.setSubMenuOrder(defaultOrder(requestDto.getSubMenuOrder()));

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
                .sorted(Comparator.comparingInt((SysSubMenu subMenu) -> defaultOrder(subMenu.getMenuOrder()))
                        .thenComparingInt(subMenu -> defaultOrder(subMenu.getSubMenuOrder()))
                        .thenComparing(SysSubMenu::getModuleCode)
                        .thenComparing(SysSubMenu::getSubmoduleCode)
                        .thenComparing(SysSubMenu::getFeatureTypeCode)
                        .thenComparing(SysSubMenu::getFeatureCode)
                        .thenComparing(SysSubMenu::getName))
                .map(SubMenuDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ApplicationContextDto getApplicationContext(String username) {
        Set<String> privilegeCodes = getUserPrivilegeCodes(username);
        ClientApplicationContextDto clientContext = clientApplicationContextService.getCurrentClientContext().orElse(null);
        ApplicationContextDto context = ApplicationContextDto.builder()
                .clientCode(clientContext == null ? null : clientContext.getClientCode())
                .clientType(clientContext == null ? null : clientContext.getClientType().name())
                .menus(getUserSidebarMenu(username))
                .privilegeCodes(privilegeCodes)
                .enabledModules(resolveEnabledModules(privilegeCodes))
                .enabledSubmodules(resolveEnabledSubmodules(privilegeCodes))
                .enabledFeatures(resolveEnabledFeatures(privilegeCodes))
                .build();
        return authApplicationContextService.applyAuthPolicy(context, null);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SidebarMenuDto> getUserSidebarMenu(String username) {
        AuthUserAccessDto userAccess = authModuleGateway.getUserAccess(username);

        Set<String> userPrivilegeCodes = getUserPrivilegeCodes(username);

        return List.of(
                buildMainMenu(FeatureType.SETUP, "fa fa-sliders", userPrivilegeCodes, userAccess.admin()),
                buildMainMenu(FeatureType.OPERATIONS, "fa fa-briefcase", userPrivilegeCodes, userAccess.admin()),
                buildMainMenu(FeatureType.REPORT, "fa fa-chart-line", userPrivilegeCodes, userAccess.admin())
        ).stream()
                .sorted(Comparator.comparingInt(menu -> defaultOrder(menu.getMenuOrder())))
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<String> getUserPrivilegeCodes(String username) {
        AuthUserAccessDto userAccess = authModuleGateway.getUserAccess(username);

        Set<String> directPrivileges = new HashSet<>(privilegeRepository.findActivePrivilegeCodesByUserId(userAccess.userId()));

        Set<String> rolePrivileges = userAccess.roleIds().isEmpty()
                ? Set.of()
                : new HashSet<>(privilegeRepository.findActivePrivilegeCodesByRoleIdIn(userAccess.roleIds()));

        Set<String> privilegeCodes = Stream.concat(directPrivileges.stream(), rolePrivileges.stream())
                .collect(Collectors.toSet());

        return clientApplicationContextService.getCurrentClientPrivilegeCodes(privilegeCodes);
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

        authModuleGateway.requireRoleExists(requestDto.getRoleId());

        Set<SysPrivilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        rolePrivilegeRepository.deleteByIdRoleId(requestDto.getRoleId());
        rolePrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> SysRolePrivilege.builder()
                        .id(new RolePrivilegeId(requestDto.getRoleId(), privilege.getId()))
                        .build())
                .toList());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        authModuleGateway.requireUserExists(requestDto.getUserId());

        Set<SysPrivilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        userPrivilegeRepository.deleteByIdUserId(requestDto.getUserId());
        userPrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> SysUserPrivilege.builder()
                        .id(new UserPrivilegeId(requestDto.getUserId(), privilege.getId()))
                        .build())
                .toList());
    }

    private Set<SysPrivilege> loadPrivileges(Set<String> privilegeCodes) {
        if (privilegeCodes == null || privilegeCodes.isEmpty()) {
            return new HashSet<>();
        }

        List<SysPrivilege> privileges = privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
        if (privileges.size() != privilegeCodes.size()) {
            Set<String> foundCodes = privileges.stream()
                    .map(SysPrivilege::getPrivilegeCode)
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
        List<SysPrivilege> privileges = admin
                ? privilegeRepository.findAll()
                : privilegeRepository.findByPrivilegeCodeIn(userPrivilegeCodes);
        if (admin && clientApplicationContextService.hasCurrentClient()) {
            Set<String> allPrivilegeCodes = privileges.stream()
                    .map(SysPrivilege::getPrivilegeCode)
                    .collect(Collectors.toSet());
            Set<String> clientPrivilegeCodes = clientApplicationContextService.getCurrentClientPrivilegeCodes(allPrivilegeCodes);
            privileges = privileges.stream()
                    .filter(privilege -> clientPrivilegeCodes.contains(privilege.getPrivilegeCode()))
                    .toList();
        }

        Map<Long, SidebarMenuDto> childMenus = new LinkedHashMap<>();
        privileges.stream()
                .filter(SysPrivilege::isActive)
                .filter(privilege -> privilege.getSubMenu() != null)
                .filter(privilege -> privilege.getSubMenu().isActive())
                .filter(privilege -> featureType.getCode().equals(privilege.getSubMenu().getFeatureTypeCode()))
                .sorted(Comparator.comparingInt((SysPrivilege privilege) -> defaultOrder(privilege.getSubMenu().getMenuOrder()))
                        .thenComparingInt(privilege -> defaultOrder(privilege.getSubMenu().getSubMenuOrder()))
                        .thenComparing((SysPrivilege privilege) -> privilege.getSubMenu().getModuleCode())
                        .thenComparing((SysPrivilege privilege) -> privilege.getSubMenu().getSubmoduleCode())
                        .thenComparing((SysPrivilege privilege) -> privilege.getSubMenu().getFeatureCode())
                        .thenComparing((SysPrivilege privilege) -> privilege.getSubMenu().getName()))
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
                .menuOrder(resolveMainMenuOrder(featureMenus))
                .children(featureMenus)
                .build();
    }

    private SidebarMenuDto toSidebarMenu(SysSubMenu subMenu) {
        return SidebarMenuDto.builder()
                .label(subMenu.getName())
                .icon(subMenu.getIcon())
                .path(subMenu.getUrl())
                .menuOrder(defaultOrder(subMenu.getMenuOrder()))
                .subMenuOrder(defaultOrder(subMenu.getSubMenuOrder()))
                .build();
    }

    private int resolveMainMenuOrder(List<SidebarMenuDto> childMenus) {
        return childMenus.stream()
                .map(SidebarMenuDto::getMenuOrder)
                .mapToInt(this::defaultOrder)
                .min()
                .orElse(0);
    }

    private SysSubMenu resolveSubMenu(Long subMenuId) {
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

    private Set<String> resolveEnabledModules(Set<String> privilegeCodes) {
        return privilegeRepository.findByPrivilegeCodeIn(privilegeCodes).stream()
                .map(SysPrivilege::getModuleCode)
                .collect(Collectors.toSet());
    }

    private Set<String> resolveEnabledSubmodules(Set<String> privilegeCodes) {
        return privilegeRepository.findByPrivilegeCodeIn(privilegeCodes).stream()
                .map(privilege -> privilege.getModuleCode() + ":" + privilege.getSubmoduleCode())
                .collect(Collectors.toSet());
    }

    private Set<String> resolveEnabledFeatures(Set<String> privilegeCodes) {
        return privilegeRepository.findByPrivilegeCodeIn(privilegeCodes).stream()
                .map(privilege -> privilege.getModuleCode()
                        + ":"
                        + privilege.getSubmoduleCode()
                        + ":"
                        + privilege.getFeatureTypeCode()
                        + ":"
                        + privilege.getFeatureCode())
                .collect(Collectors.toSet());
    }

    private int defaultOrder(Integer order) {
        return order == null ? 0 : order;
    }

    private SysFeature resolveFeature(PrivilegeRequestDto requestDto) {
        return resolveFeature(
                requestDto.getModuleCode(),
                requestDto.getModuleName(),
                requestDto.getSubmoduleCode(),
                requestDto.getSubmoduleName(),
                requestDto.getFeatureTypeCode(),
                requestDto.getFeatureTypeName(),
                requestDto.getFeatureCode(),
                requestDto.getFeatureName()
        );
    }

    private SysFeature resolveFeature(SubMenuRequestDto requestDto) {
        return resolveFeature(
                requestDto.getModuleCode(),
                requestDto.getModuleName(),
                requestDto.getSubmoduleCode(),
                requestDto.getSubmoduleName(),
                requestDto.getFeatureTypeCode(),
                requestDto.getFeatureTypeName(),
                requestDto.getFeatureCode(),
                requestDto.getFeatureName()
        );
    }

    private SysFeature resolveFeature(String moduleCode,
                                      String moduleName,
                                      String submoduleCode,
                                      String submoduleName,
                                      String featureTypeCode,
                                      String featureTypeName,
                                      String featureCode,
                                      String featureName) {
        SysModule module = moduleRepository.findByCode(moduleCode)
                .orElseGet(() -> moduleRepository.save(SysModule.builder()
                        .code(moduleCode)
                        .name(moduleName)
                        .active(true)
                        .build()));
        if (!Objects.equals(moduleName, module.getName()) || !module.isActive()) {
            module.setName(moduleName);
            module.setActive(true);
            module = moduleRepository.save(module);
        }

        SysModule resolvedModule = module;
        SysSubmodule submodule = submoduleRepository.findByModuleCodeAndCode(moduleCode, submoduleCode)
                .orElseGet(() -> submoduleRepository.save(SysSubmodule.builder()
                        .module(resolvedModule)
                        .code(submoduleCode)
                        .name(submoduleName)
                        .active(true)
                        .build()));
        if (!Objects.equals(submoduleName, submodule.getName()) || !submodule.isActive()) {
            submodule.setName(submoduleName);
            submodule.setActive(true);
            submodule = submoduleRepository.save(submodule);
        }

        SysSubmodule resolvedSubmodule = submodule;
        return featureRepository.findBySubmoduleModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndCode(
                        moduleCode,
                        submoduleCode,
                        featureTypeCode,
                        featureCode
                )
                .map(feature -> {
                    if (!Objects.equals(featureName, feature.getName())
                            || !Objects.equals(featureTypeName, feature.getFeatureTypeName())
                            || !feature.isActive()) {
                        feature.setName(featureName);
                        feature.setFeatureTypeName(featureTypeName);
                        feature.setActive(true);
                        return featureRepository.save(feature);
                    }
                    return feature;
                })
                .orElseGet(() -> featureRepository.save(SysFeature.builder()
                        .submodule(resolvedSubmodule)
                        .featureTypeCode(featureTypeCode)
                        .featureTypeName(featureTypeName)
                        .code(featureCode)
                        .name(featureName)
                        .active(true)
                        .build()));
    }

    private void validateCodePart(String value, int expectedLength, String fieldName) {
        if (value == null || !value.matches("\\d{" + expectedLength + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + expectedLength + " digits");
        }
    }
}
