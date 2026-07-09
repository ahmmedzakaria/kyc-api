package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationDto;
import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.clientaccess.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;

import java.util.List;

public interface ClientApplicationService {
    ClientApplicationDto save(ClientApplicationRequestDto requestDto, String username);

    List<ClientApplicationDto> list();

    SysClientApplication requireClientApplication(Long clientApplicationId, String clientCode);

    GeneratedClientCredentialDto rotateApiKey(Long clientApplicationId, String clientCode, String username);
}
