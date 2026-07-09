package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;

import java.util.Set;

public interface ClientAccessDecisionService {
    ClientAccessDecisionDto decide(SysClientApplication clientApplication, SysApiRegistry apiRegistry);

    Set<String> filterPrivilegeCodesForClient(SysClientApplication clientApplication, Set<String> userPrivilegeCodes);
}
