package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface ClientApiRegistryService {
    ApiRegistryDto save(ApiRegistryRequestDto requestDto, String username);

    List<ApiRegistryDto> list();

    ApiRegistrySyncReportDto syncFromAnnotations(String username);

    Optional<SysPrivApiRegistry> resolve(HttpServletRequest request);
}
