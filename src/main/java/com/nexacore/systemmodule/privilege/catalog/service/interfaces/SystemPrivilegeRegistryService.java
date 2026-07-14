package com.nexacore.systemmodule.privilege.catalog.service.interfaces;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubMenu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SystemPrivilegeRegistryService {
    void syncApplicationCatalog();

    long countPrivileges();

    Optional<SysPrivilege> findPrivilegeByCode(String privilegeCode);

    List<SysPrivilege> findPrivilegesByCodes(Collection<String> privilegeCodes);

    List<SysPrivilege> getAllPrivileges();

    SysPrivilege savePrivilege(SysPrivilege privilege);

    Optional<SysSubMenu> findSubMenu(String moduleCode,
                                  String submoduleCode,
                                  String featureTypeCode,
                                  String featureCode,
                                  String url);

    SysSubMenu saveSubMenu(SysSubMenu subMenu);

    void assignRolePrivileges(Long roleId, Collection<String> privilegeCodes);

    long countRolePrivileges(Long roleId);

    long countSubMenus();
}
