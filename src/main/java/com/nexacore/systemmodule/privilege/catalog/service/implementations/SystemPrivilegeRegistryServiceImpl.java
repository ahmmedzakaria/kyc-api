package com.nexacore.systemmodule.privilege.catalog.service.implementations;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.RolePrivilegeId;
import com.nexacore.systemmodule.privilege.catalog.entity.SysFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.SystemPrivilegeRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemPrivilegeRegistryServiceImpl implements SystemPrivilegeRegistryService {

    private final PrivilegeRepository privilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final SubMenuRepository subMenuRepository;
    private final ModuleRepository moduleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final FeatureRepository featureRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void syncApplicationCatalog() {
        for (ApplicationModule applicationModule : ApplicationModule.values()) {
            resolveModule(applicationModule.getCode(), applicationModule.getDisplayName());
        }
        for (ApplicationSubmodule applicationSubmodule : ApplicationSubmodule.values()) {
            SysModule module = resolveModule(
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
    public Optional<SysPrivilege> findPrivilegeByCode(String privilegeCode) {
        return privilegeRepository.findByPrivilegeCode(privilegeCode);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SysPrivilege> findPrivilegesByCodes(Collection<String> privilegeCodes) {
        return privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<SysPrivilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public SysPrivilege savePrivilege(SysPrivilege privilege) {
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
        return privilegeRepository.save(privilege);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysSubMenu> findSubMenu(String moduleCode,
                                         String submoduleCode,
                                         String featureTypeCode,
                                         String featureCode,
                                         String url) {
        return subMenuRepository.findFirstByFeatureAndUrl(
                moduleCode,
                submoduleCode,
                featureTypeCode,
                featureCode,
                url
        );
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public SysSubMenu saveSubMenu(SysSubMenu subMenu) {
        if (subMenu.getFeature() == null) {
            subMenu.setFeature(resolveFeature(
                    subMenu.getModuleCode(),
                    subMenu.getModuleName(),
                    subMenu.getSubmoduleCode(),
                    subMenu.getSubmoduleName(),
                    subMenu.getFeatureTypeCode(),
                    subMenu.getFeatureTypeName(),
                    subMenu.getFeatureCode(),
                    subMenu.getFeatureName()
            ));
        }
        return subMenuRepository.save(subMenu);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void assignRolePrivileges(Long roleId, Collection<String> privilegeCodes) {
        rolePrivilegeRepository.deleteByIdRoleId(roleId);
        rolePrivilegeRepository.saveAll(privilegeRepository.findByPrivilegeCodeIn(privilegeCodes).stream()
                .map(privilege -> SysRolePrivilege.builder()
                        .id(new RolePrivilegeId(roleId, privilege.getId()))
                        .build())
                .toList());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public long countRolePrivileges(Long roleId) {
        return rolePrivilegeRepository.countByIdRoleId(roleId);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public long countSubMenus() {
        return subMenuRepository.count();
    }

    private SysFeature resolveFeature(String moduleCode,
                                      String moduleName,
                                      String submoduleCode,
                                      String submoduleName,
                                      String featureTypeCode,
                                      String featureTypeName,
                                      String featureCode,
                                      String featureName) {
        SysModule module = resolveModule(moduleCode, moduleName);
        SysSubmodule submodule = resolveSubmodule(module, submoduleCode, submoduleName);

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

    private SysModule resolveModule(String moduleCode, String moduleName) {
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
        return module;
    }

    private SysSubmodule resolveSubmodule(SysModule module, String submoduleCode, String submoduleName) {
        SysSubmodule submodule = submoduleRepository.findByModuleCodeAndCode(module.getCode(), submoduleCode)
                .orElseGet(() -> submoduleRepository.save(SysSubmodule.builder()
                        .module(module)
                        .code(submoduleCode)
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
