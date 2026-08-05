package com.nexacore.systemmodule.privilege.catalog.service.interfaces;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubMenu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SystemPrivilegeRegistryService {
    void syncApplicationCatalog();

    long countPrivileges();

    Optional<SysPrivPrivilege> findPrivilegeByCode(String privilegeCode);

    List<SysPrivPrivilege> findPrivilegesByCodes(Collection<String> privilegeCodes);

    List<SysPrivPrivilege> getAllPrivileges();

    SysPrivPrivilege savePrivilege(SysPrivPrivilege privilege);

    Optional<SysPrivSubMenu> findSubMenu(String moduleCode,
                                  String submoduleCode,
                                  String featureTypeCode,
                                  String featureCode,
                                  String url);

    SysPrivSubMenu saveSubMenu(SysPrivSubMenu subMenu);

    void assignRolePrivileges(Long roleId, Collection<String> privilegeCodes);

    long countRolePrivileges(Long roleId);

    long countSubMenus();
}
