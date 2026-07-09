package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.dto.ApiRegistryDto;
import com.nexacore.systemmodule.clientaccess.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface ClientApiRegistryService {
    ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username);

    List<ApiRegistryDto> list();

    List<ApiRegistryDto> syncFromAnnotations(String username);

    Optional<SysApiRegistry> resolve(HttpServletRequest request);
}
