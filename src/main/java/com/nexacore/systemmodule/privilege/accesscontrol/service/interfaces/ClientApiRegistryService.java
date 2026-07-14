package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.privilege.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface ClientApiRegistryService {
    ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username);

    List<ApiRegistryDto> list();

    List<ApiRegistryDto> syncFromAnnotations(String username);

    Optional<SysApiRegistry> resolve(HttpServletRequest request);
}
