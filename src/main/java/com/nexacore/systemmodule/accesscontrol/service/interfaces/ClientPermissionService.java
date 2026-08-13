package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientAdministrationDetailDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientScopeAssignmentDto;
import java.util.Set;

public interface ClientPermissionService {
    void assignApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void grantApiPermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void assignFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void grantFeaturePermissions(ClientPermissionAssignmentRequestDto requestDto, String username);

    void assignTenants(ClientPermissionAssignmentRequestDto requestDto, String username);

    ClientAdministrationDetailDto getAdministrationDetail(Long clientApplicationId, String clientCode);

    Set<Long> getApiPermissions(Long clientApplicationId, String clientCode);

    Set<String> getFeaturePermissions(Long clientApplicationId, String clientCode);

    Set<ClientScopeAssignmentDto> getScopeAssignments(Long clientApplicationId, String clientCode);
}
