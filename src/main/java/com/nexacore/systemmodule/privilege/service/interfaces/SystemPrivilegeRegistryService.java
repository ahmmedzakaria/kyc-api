package com.nexacore.systemmodule.privilege.service.interfaces;

import com.nexacore.systemmodule.privilege.entity.Privilege;
import com.nexacore.systemmodule.privilege.entity.SubMenu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SystemPrivilegeRegistryService {
    long countPrivileges();

    Optional<Privilege> findPrivilegeByCode(String privilegeCode);

    List<Privilege> findPrivilegesByCodes(Collection<String> privilegeCodes);

    List<Privilege> getAllPrivileges();

    Privilege savePrivilege(Privilege privilege);

    Optional<SubMenu> findSubMenu(String moduleCode,
                                  String submoduleCode,
                                  String featureTypeCode,
                                  String featureCode,
                                  String url);

    SubMenu saveSubMenu(SubMenu subMenu);

    void assignRolePrivileges(Long roleId, Collection<String> privilegeCodes);

    long countRolePrivileges(Long roleId);

    long countSubMenus();
}
