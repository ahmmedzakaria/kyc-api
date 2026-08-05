package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ApiInventoryItemDto;

import java.util.List;

public interface ApiInventoryService {
    List<ApiInventoryItemDto> inventory();
}
