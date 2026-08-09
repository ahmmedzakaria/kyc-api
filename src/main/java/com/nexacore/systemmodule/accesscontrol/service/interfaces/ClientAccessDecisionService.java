package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;

import java.util.Set;

public interface ClientAccessDecisionService {
    ClientAccessDecisionDto decide(SysAccClientApplication clientApplication, SysAccApiRegistry apiRegistry);

    Set<String> filterPrivilegeCodesForClient(SysAccClientApplication clientApplication, Set<String> userPrivilegeCodes);
}
