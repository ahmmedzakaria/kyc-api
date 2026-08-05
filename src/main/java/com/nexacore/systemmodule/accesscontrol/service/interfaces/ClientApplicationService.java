package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;

import java.util.List;

public interface ClientApplicationService {
    ClientApplicationDto save(ClientApplicationRequestDto requestDto, String username);

    List<ClientApplicationDto> list();

    SysPrivClientApplication requireClientApplication(Long clientApplicationId, String clientCode);

    GeneratedClientCredentialDto rotateApiKey(Long clientApplicationId, String clientCode, String username);
}
