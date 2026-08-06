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
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationContextDto;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationContextService;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeatureType;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivAction;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilegeId;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubMenu;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilegeId;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutRoutePolicyService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutUiPolicyService;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureTypeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ActionRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.UserPrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.ModulePrivilegeProvider;
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
    private final FeatureTypeRepository featureTypeRepository;
    private final ActionRepository actionRepository;
    private final AuthModuleGateway authModuleGateway;
    private final SubMenuRepository subMenuRepository;
    private final List<ModulePrivilegeProvider> modulePrivilegeProviders;
    private final ClientApplicationContextService clientApplicationContextService;
    private final AuthApplicationContextService authApplicationContextService;
    private final LayoutContextService layoutContextService;
    private final LayoutRoutePolicyService layoutRoutePolicyService;
    private final LayoutUiPolicyService layoutUiPolicyService;
    private final AuthorizationDataCache authorizationDataCache;

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

        SysPrivPrivilege privilege = privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(SysPrivPrivilege::new);

        privilege.setPrivilegeCode(privilegeCode);
        privilege.setFeature(resolveFeature(requestDto));
        privilege.setAction(resolveAction(requestDto.getActionCode(), requestDto.getActionName()));
        privilege.setSubMenu(resolveSubMenu(requestDto.getSubMenuId()));
        privilege.setActive(requestDto.getActive() == null || requestDto.getActive());

        PrivilegeDto saved = PrivilegeDto.fromEntity(privilegeRepository.save(privilege));
        authorizationDataCache.invalidateAllUserPrivilegesAfterCommit();
        return saved;
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

        SysPrivSubMenu subMenu = requestDto.getId() == null
                ? new SysPrivSubMenu()
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
                .sorted(Comparator.comparingInt((SysPrivSubMenu subMenu) -> defaultOrder(subMenu.getMenuOrder()))
                        .thenComparingInt(subMenu -> defaultOrder(subMenu.getSubMenuOrder()))
                        .thenComparing(SysPrivSubMenu::getModuleCode)
                        .thenComparing(SysPrivSubMenu::getSubmoduleCode)
                        .thenComparing(SysPrivSubMenu::getFeatureTypeCode)
                        .thenComparing(SysPrivSubMenu::getFeatureCode)
                        .thenComparing(SysPrivSubMenu::getName))
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
                .privilegeCodes(privilegeCodes)
                .enabledModules(resolveEnabledModules(privilegeCodes))
                .enabledSubmodules(resolveEnabledSubmodules(privilegeCodes))
                .enabledFeatures(resolveEnabledFeatures(privilegeCodes))
                .routePolicies(layoutRoutePolicyService.getEffectivePolicies(
                        clientContext == null ? null : clientContext.getClientCode()
                ))
                .uiPolicies(layoutUiPolicyService.getEffectivePolicies(
                        clientContext == null ? null : clientContext.getClientCode()
                ))
                .layout(layoutContextService.getEffectiveLayout(
                        clientContext == null ? null : clientContext.getClientCode(),
                        username,
                        privilegeCodes
                ))
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

        ClientApplicationContextDto client = clientApplicationContextService.getCurrentClientContext().orElse(null);
        return authorizationDataCache.userPrivileges(userAccess.userId(), client == null ? null : client.getClientApplicationId(), () -> {
            Set<String> directPrivileges = new HashSet<>(privilegeRepository.findActivePrivilegeCodesByUserId(userAccess.userId()));
            Set<String> rolePrivileges = userAccess.roleIds().isEmpty() ? Set.of()
                    : new HashSet<>(privilegeRepository.findActivePrivilegeCodesByRoleIdIn(userAccess.roleIds()));
            Set<String> privilegeCodes = Stream.concat(directPrivileges.stream(), rolePrivileges.stream()).collect(Collectors.toSet());
            return clientApplicationContextService.getCurrentClientPrivilegeCodes(privilegeCodes);
        });
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

        Set<SysPrivPrivilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        rolePrivilegeRepository.deleteByIdRoleId(requestDto.getRoleId());
        rolePrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> SysPrivRolePrivilege.builder()
                        .id(new SysPrivRolePrivilegeId(requestDto.getRoleId(), privilege.getId()))
                        .build())
                .toList());
        authorizationDataCache.invalidateAllUserPrivilegesAfterCommit();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto) {
        if (requestDto.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        authModuleGateway.requireUserExists(requestDto.getUserId());

        Set<SysPrivPrivilege> privileges = loadPrivileges(requestDto.getPrivilegeCodes());
        userPrivilegeRepository.deleteByIdUserId(requestDto.getUserId());
        userPrivilegeRepository.saveAll(privileges.stream()
                .map(privilege -> SysPrivUserPrivilege.builder()
                        .id(new SysPrivUserPrivilegeId(requestDto.getUserId(), privilege.getId()))
                        .build())
                .toList());
        authorizationDataCache.invalidateUserAfterCommit(requestDto.getUserId());
    }

    private Set<SysPrivPrivilege> loadPrivileges(Set<String> privilegeCodes) {
        if (privilegeCodes == null || privilegeCodes.isEmpty()) {
            return new HashSet<>();
        }

        List<SysPrivPrivilege> privileges = privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
        if (privileges.size() != privilegeCodes.size()) {
            Set<String> foundCodes = privileges.stream()
                    .map(SysPrivPrivilege::getPrivilegeCode)
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
        List<SysPrivPrivilege> privileges = admin
                ? privilegeRepository.findAll()
                : privilegeRepository.findByPrivilegeCodeIn(userPrivilegeCodes);
        if (admin && clientApplicationContextService.hasCurrentClient()) {
            Set<String> allPrivilegeCodes = privileges.stream()
                    .map(SysPrivPrivilege::getPrivilegeCode)
                    .collect(Collectors.toSet());
            Set<String> clientPrivilegeCodes = clientApplicationContextService.getCurrentClientPrivilegeCodes(allPrivilegeCodes);
            privileges = privileges.stream()
                    .filter(privilege -> clientPrivilegeCodes.contains(privilege.getPrivilegeCode()))
                    .toList();
        }

        Map<Long, SidebarMenuDto> childMenus = new LinkedHashMap<>();
        privileges.stream()
                .filter(SysPrivPrivilege::isActive)
                .filter(privilege -> privilege.getSubMenu() != null)
                .filter(privilege -> privilege.getSubMenu().isActive())
                .filter(privilege -> featureType.getCode().equals(privilege.getSubMenu().getFeatureTypeCode()))
                .sorted(Comparator.comparingInt((SysPrivPrivilege privilege) -> defaultOrder(privilege.getSubMenu().getMenuOrder()))
                        .thenComparingInt(privilege -> defaultOrder(privilege.getSubMenu().getSubMenuOrder()))
                        .thenComparing((SysPrivPrivilege privilege) -> privilege.getSubMenu().getModuleCode())
                        .thenComparing((SysPrivPrivilege privilege) -> privilege.getSubMenu().getSubmoduleCode())
                        .thenComparing((SysPrivPrivilege privilege) -> privilege.getSubMenu().getFeatureCode())
                        .thenComparing((SysPrivPrivilege privilege) -> privilege.getSubMenu().getName()))
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

    private SidebarMenuDto toSidebarMenu(SysPrivSubMenu subMenu) {
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

    private SysPrivSubMenu resolveSubMenu(Long subMenuId) {
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
                .map(SysPrivPrivilege::getModuleCode)
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

    private SysPrivFeature resolveFeature(PrivilegeRequestDto requestDto) {
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

    private SysPrivFeature resolveFeature(SubMenuRequestDto requestDto) {
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

    private SysPrivFeature resolveFeature(String moduleCode,
                                      String moduleName,
                                      String submoduleCode,
                                      String submoduleName,
                                      String featureTypeCode,
                                      String featureTypeName,
                                      String featureCode,
                                      String featureName) {
        SysPrivModule module = moduleRepository.findByCode(moduleCode)
                .orElseGet(() -> moduleRepository.save(SysPrivModule.builder()
                        .code(moduleCode)
                        .name(moduleName)
                        .active(true)
                        .build()));
        if (!Objects.equals(moduleName, module.getName()) || !module.isActive()) {
            module.setName(moduleName);
            module.setActive(true);
            module = moduleRepository.save(module);
        }

        SysPrivModule resolvedModule = module;
        SysPrivSubmodule submodule = submoduleRepository.findByModuleCodeAndCode(moduleCode, submoduleCode)
                .orElseGet(() -> submoduleRepository.save(SysPrivSubmodule.builder()
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

        SysPrivSubmodule resolvedSubmodule = submodule;
        SysPrivFeatureType featureType = resolveFeatureType(featureTypeCode, featureTypeName);
        return featureRepository.findBySubmoduleModuleCodeAndSubmoduleCodeAndFeatureTypeFeatureTypeCodeAndFeatureCode(
                        moduleCode,
                        submoduleCode,
                        featureTypeCode,
                        featureCode
                )
                .map(feature -> {
                    if (!Objects.equals(featureName, feature.getFeatureName()) || !feature.isActive()) {
                        feature.setFeatureName(featureName);
                        feature.setActive(true);
                        return featureRepository.save(feature);
                    }
                    return feature;
                })
                .orElseGet(() -> featureRepository.save(SysPrivFeature.builder()
                        .submodule(resolvedSubmodule)
                        .featureType(featureType)
                        .featureCode(featureCode)
                        .featureName(featureName)
                        .active(true)
                        .build()));
    }

    private SysPrivFeatureType resolveFeatureType(String code, String name) {
        SysPrivFeatureType featureType = featureTypeRepository.findByTenantIdIsNullAndFeatureTypeCode(code)
                .orElseGet(() -> featureTypeRepository.save(SysPrivFeatureType.builder()
                        .featureTypeCode(code)
                        .featureTypeName(name)
                        .active(true)
                        .createdBy(0L)
                        .updatedBy(0L)
                        .build()));
        if (!Objects.equals(name, featureType.getFeatureTypeName()) || !featureType.isActive()) {
            featureType.setFeatureTypeName(name);
            featureType.setActive(true);
            featureType.setUpdatedBy(0L);
            featureType = featureTypeRepository.save(featureType);
        }
        return featureType;
    }

    private SysPrivAction resolveAction(String code, String name) {
        SysPrivAction action = actionRepository.findByActionCode(code)
                .orElseGet(() -> actionRepository.save(SysPrivAction.builder()
                        .actionCode(code)
                        .actionName(name)
                        .displayOrder(Integer.valueOf(code))
                        .active(true)
                        .createdBy(0L)
                        .updatedBy(0L)
                        .build()));
        if (!Objects.equals(name, action.getActionName()) || !action.isActive()) {
            action.setActionName(name);
            action.setActive(true);
            action.setUpdatedBy(0L);
            action = actionRepository.save(action);
        }
        return action;
    }

    private void validateCodePart(String value, int expectedLength, String fieldName) {
        if (value == null || !value.matches("\\d{" + expectedLength + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + expectedLength + " digits");
        }
    }
}
