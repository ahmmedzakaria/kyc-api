package com.nexacore.systemmodule.publicnavigation.service;

import com.nexacore.gatewaymodule.navigation.dto.PublicNavigationContextDto;
import com.nexacore.systemmodule.publicnavigation.dto.*;

import java.util.List;

public interface PublicNavigationAdministrationService {
    List<PublicNavigationGroupDto> listGroups(PublicNavigationScopeRequest request);
    PublicNavigationGroupDto saveGroup(PublicNavigationGroupRequest request, String username);
    List<PublicNavigationItemDto> listItems(PublicNavigationScopeRequest request);
    PublicNavigationItemDto saveItem(PublicNavigationItemRequest request, String username);
    PublicNavigationItemDto deactivateItem(Long tenantId, Long itemId, Long version, String username);
    void deleteItem(Long tenantId, Long itemId);
    PublicNavigationAssignmentDto listAssignments(PublicNavigationScopeRequest request);
    PublicNavigationAssignmentDto replaceAssignments(PublicNavigationAssignmentRequest request, String username);
    PublicNavigationIntegrityDto integrity(Long tenantId);
    PublicNavigationContextDto preview(PublicNavigationScopeRequest request);
}
