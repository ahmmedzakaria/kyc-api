package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;

import java.util.Set;

public interface ClientAccessDecisionService {
    ClientAccessDecisionDto decide(SysPrivClientApplication clientApplication, SysPrivApiRegistry apiRegistry);

    Set<String> filterPrivilegeCodesForClient(SysPrivClientApplication clientApplication, Set<String> userPrivilegeCodes);
}
