package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysRolePrivilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilegeId;
import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.service.interfaces.SystemPrivilegeRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemPrivilegeRegistryServiceImpl implements SystemPrivilegeRegistryService {

    private final PrivilegeRepository privilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final SubMenuRepository subMenuRepository;

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
        return privilegeRepository.save(privilege);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysSubMenu> findSubMenu(String moduleCode,
                                         String submoduleCode,
                                         String featureTypeCode,
                                         String featureCode,
                                         String url) {
        return subMenuRepository.findFirstByModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndFeatureCodeAndUrl(
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
}
