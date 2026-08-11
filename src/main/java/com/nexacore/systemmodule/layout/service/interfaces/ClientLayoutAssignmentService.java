package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentDto;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutTenantReconciliationDto;

import java.util.List;

public interface ClientLayoutAssignmentService {
    ClientLayoutAssignmentDto assign(ClientLayoutAssignmentRequestDto request, String actor);
    List<ClientLayoutAssignmentDto> list(Long tenantId, String clientCode);
    LayoutTenantReconciliationDto reconcile(Long tenantId);
}
