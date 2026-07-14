package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientApplicationDto;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;

import java.util.List;

public interface ClientApplicationService {
    ClientApplicationDto save(ClientApplicationRequestDto requestDto, String username);

    List<ClientApplicationDto> list();

    SysClientApplication requireClientApplication(Long clientApplicationId, String clientCode);

    GeneratedClientCredentialDto rotateApiKey(Long clientApplicationId, String clientCode, String username);
}
