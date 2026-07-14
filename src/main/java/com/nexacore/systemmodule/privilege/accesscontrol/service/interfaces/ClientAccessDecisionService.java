package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;

import java.util.Set;

public interface ClientAccessDecisionService {
    ClientAccessDecisionDto decide(SysClientApplication clientApplication, SysApiRegistry apiRegistry);

    Set<String> filterPrivilegeCodesForClient(SysClientApplication clientApplication, Set<String> userPrivilegeCodes);
}
