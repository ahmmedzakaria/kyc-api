package com.nexacore.systemmodule.privilege.catalog.service.implementations;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilegeId;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationCategoryRepository;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivAction;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ActionRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.SystemPrivilegeRegistryService;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SystemPrivilegeRegistryServiceImpl implements SystemPrivilegeRegistryService {

    private final PrivilegeRepository privilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final ModuleRepository moduleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final FeatureRepository featureRepository;
    private final LayoutNavigationCategoryRepository navigationCategoryRepository;
    private final ActionRepository actionRepository;
    private final AuthorizationDataCache authorizationDataCache;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void syncApplicationCatalog() {
        for (ApplicationModule applicationModule : ApplicationModule.values()) {
            resolveModule(applicationModule.getCode(), applicationModule.getDisplayName());
        }
        for (ApplicationSubmodule applicationSubmodule : ApplicationSubmodule.values()) {
            SysPrivModule module = resolveModule(
                    applicationSubmodule.getModule().getCode(),
                    applicationSubmodule.getModule().getDisplayName()
            );
            resolveSubmodule(module, applicationSubmodule.getCode(), applicationSubmodule.getDisplayName());
        }
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public long countPrivileges() {
        return privilegeRepository.count();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysPrivPrivilege> findPrivilegeByCode(String privilegeCode) {
        return privilegeRepository.findByPrivilegeCode(privilegeCode);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SysPrivPrivilege> findPrivilegesByCodes(Collection<String> privilegeCodes) {
        return privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SysPrivPrivilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public SysPrivPrivilege savePrivilege(SysPrivPrivilege privilege) {
        if (privilege.getFeature() == null) {
            privilege.setFeature(resolveFeature(
                    privilege.getModuleCode(),
                    privilege.getModuleName(),
                    privilege.getSubmoduleCode(),
                    privilege.getSubmoduleName(),
                    privilege.getFeatureTypeCode(),
                    privilege.getFeatureTypeName(),
                    privilege.getFeatureCode(),
                    privilege.getFeatureName()
            ));
        }
        if (privilege.getAction() == null) {
            privilege.setAction(resolveAction(privilege.getActionCode(), privilege.getActionName()));
        }
        SysPrivPrivilege saved = privilegeRepository.save(privilege);
        authorizationDataCache.invalidateAllUserPrivilegesAfterCommit();
        return saved;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignRolePrivileges(Long roleId, Collection<String> privilegeCodes) {
        rolePrivilegeRepository.deleteByIdRoleId(roleId);
        grantRolePrivileges(roleId, privilegeCodes);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void grantRolePrivileges(Long roleId, Collection<String> privilegeCodes) {
        rolePrivilegeRepository.saveAll(privilegeRepository.findByPrivilegeCodeIn(privilegeCodes).stream()
                .map(privilege -> SysPrivRolePrivilege.builder()
                        .id(new SysPrivRolePrivilegeId(roleId, privilege.getId()))
                        .build())
                .toList());
        authorizationDataCache.invalidateAllUserPrivilegesAfterCommit();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public long countRolePrivileges(Long roleId) {
        return rolePrivilegeRepository.countByIdRoleId(roleId);
    }

    private SysPrivFeature resolveFeature(String moduleCode,
                                      String moduleName,
                                      String submoduleCode,
                                      String submoduleName,
                                      String featureTypeCode,
                                      String featureTypeName,
                                      String featureCode,
                                      String featureName) {
        SysPrivModule module = resolveModule(moduleCode, moduleName);
        SysPrivSubmodule submodule = resolveSubmodule(module, submoduleCode, submoduleName);

        SysPrivSubmodule resolvedSubmodule = submodule;
        SysLayoutNavigationCategory navigationCategory = resolveNavigationCategory(moduleCode, featureTypeCode);
        return featureRepository.findBySubmoduleModuleModulePrivCodeAndSubmoduleSubmodulePrivCodeAndNavigationCategoryFeatureTypeCodeAndFeatureCode(
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
                        .navigationCategory(navigationCategory)
                        .featureCode(featureCode)
                        .featureName(featureName)
                        .active(true)
                        .build()));
    }

    private SysLayoutNavigationCategory resolveNavigationCategory(String moduleCode, String featureTypeCode) {
        return navigationCategoryRepository
                .findFirstByNavigationModulePhysicalModuleCodeAndFeatureTypeCodeOrderByIdAsc(moduleCode, featureTypeCode)
                .orElseThrow(() -> new IllegalStateException(
                        "No layout navigation category for module " + moduleCode
                                + " and feature type " + featureTypeCode));
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

    private SysPrivModule resolveModule(String moduleCode, String moduleName) {
        SysPrivModule module = moduleRepository.findByModulePrivCode(moduleCode)
                .orElseGet(() -> moduleRepository.save(SysPrivModule.builder()
                        .modulePrivCode(moduleCode)
                        .name(moduleName)
                        .active(true)
                        .build()));
        if (!Objects.equals(moduleName, module.getName()) || !module.isActive()) {
            module.setName(moduleName);
            module.setActive(true);
            module = moduleRepository.save(module);
        }
        return module;
    }

    private SysPrivSubmodule resolveSubmodule(SysPrivModule module, String submoduleCode, String submoduleName) {
        SysPrivSubmodule submodule = submoduleRepository
                .findByModuleModulePrivCodeAndSubmodulePrivCode(module.getModulePrivCode(), submoduleCode)
                .orElseGet(() -> submoduleRepository.save(SysPrivSubmodule.builder()
                        .module(module)
                        .submodulePrivCode(submoduleCode)
                        .name(submoduleName)
                        .active(true)
                        .build()));
        if (!Objects.equals(submoduleName, submodule.getName()) || !submodule.isActive()) {
            submodule.setName(submoduleName);
            submodule.setActive(true);
            submodule = submoduleRepository.save(submodule);
        }
        return submodule;
    }
}
