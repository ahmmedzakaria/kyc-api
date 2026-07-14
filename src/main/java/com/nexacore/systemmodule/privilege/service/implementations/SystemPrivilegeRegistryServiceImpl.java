package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysRolePrivilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilegeId;
import com.nexacore.systemmodule.privilege.entity.SysFeature;
import com.nexacore.systemmodule.privilege.entity.SysModule;
import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.entity.SysSubmodule;
import com.nexacore.systemmodule.privilege.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.service.interfaces.SystemPrivilegeRegistryService;
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
}
