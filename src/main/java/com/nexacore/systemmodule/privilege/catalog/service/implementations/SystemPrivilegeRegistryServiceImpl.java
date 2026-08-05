package com.nexacore.systemmodule.privilege.catalog.service.implementations;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilegeId;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubMenu;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
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
        return privilegeRepository.save(privilege);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysPrivSubMenu> findSubMenu(String moduleCode,
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
    public SysPrivSubMenu saveSubMenu(SysPrivSubMenu subMenu) {
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
                .map(privilege -> SysPrivRolePrivilege.builder()
                        .id(new SysPrivRolePrivilegeId(roleId, privilege.getId()))
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
                .orElseGet(() -> featureRepository.save(SysPrivFeature.builder()
                        .submodule(resolvedSubmodule)
                        .featureTypeCode(featureTypeCode)
                        .featureTypeName(featureTypeName)
                        .code(featureCode)
                        .name(featureName)
                        .active(true)
                        .build()));
    }

    private SysPrivModule resolveModule(String moduleCode, String moduleName) {
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
        return module;
    }

    private SysPrivSubmodule resolveSubmodule(SysPrivModule module, String submoduleCode, String submoduleName) {
        SysPrivSubmodule submodule = submoduleRepository.findByModuleCodeAndCode(module.getCode(), submoduleCode)
                .orElseGet(() -> submoduleRepository.save(SysPrivSubmodule.builder()
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
