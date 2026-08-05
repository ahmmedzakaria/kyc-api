package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ApiInventoryItemDto;

import java.util.List;

public interface ApiInventoryService {
    List<ApiInventoryItemDto> inventory();
}
