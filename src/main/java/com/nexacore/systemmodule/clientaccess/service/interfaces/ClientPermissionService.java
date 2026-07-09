package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.dto.ClientPermissionAssignmentRequestDto;

public interface ClientPermissionService {
    void assignApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void assignFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void assignTenants(ClientPermissionAssignmentRequestDto requestDto, String username);
}
