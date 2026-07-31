package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentDto;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentRequestDto;
import com.nexacore.systemmodule.layout.entity.SysClientLayoutProfile;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import com.nexacore.systemmodule.layout.enums.AssignmentScope;
import com.nexacore.systemmodule.layout.enums.DeviceTarget;
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.service.interfaces.ClientLayoutAssignmentService;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ClientApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientLayoutAssignmentServiceImpl implements ClientLayoutAssignmentService {
    private final ClientLayoutProfileRepository clientLayoutProfileRepository;
    private final ClientApplicationRepository clientApplicationRepository;
    private final LayoutProfileRepository layoutProfileRepository;
    private final AuthModuleGateway authModuleGateway;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public ClientLayoutAssignmentDto assign(ClientLayoutAssignmentRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysClientApplication client = resolveClient(request);
        SysLayoutProfile profile = resolveProfile(request);
        Long currentId = request.getId() == null ? -1L : request.getId();

        if (Boolean.TRUE.equals(request.getDefaultProfile())
                && clientLayoutProfileRepository.existsByClientApplicationIdAndDefaultProfileTrueAndActiveTrueAndIdNot(client.getId(), currentId)) {
            throw new IllegalArgumentException("Client already has an active default layout profile");
        }

        SysClientLayoutProfile assignment = request.getId() == null
                ? new SysClientLayoutProfile()
                : clientLayoutProfileRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client layout assignment not found: " + request.getId()));

        assignment.setClientApplication(client);
        assignment.setLayoutProfile(profile);
        assignment.setAssignmentScope(request.getAssignmentScope() == null ? AssignmentScope.CLIENT : request.getAssignmentScope());
        assignment.setRoleCode(request.getRoleCode());
        assignment.setPrivilegeCode(request.getPrivilegeCode());
        assignment.setDeviceTarget(request.getDeviceTarget() == null ? DeviceTarget.ANY : request.getDeviceTarget());
        assignment.setModuleCode(request.getModuleCode());
        assignment.setDefaultProfile(Boolean.TRUE.equals(request.getDefaultProfile()));
        assignment.setSelectable(request.getSelectable() == null || request.getSelectable());
        assignment.setDisplayOrder(request.getDisplayOrder() == null ? 100 : request.getDisplayOrder());
        assignment.setActive(request.getActive() == null || request.getActive());
        if (assignment.getId() == null) {
            assignment.setCreatedBy(userId);
        }
        assignment.setUpdatedBy(userId);

        return toDto(clientLayoutProfileRepository.save(assignment));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<ClientLayoutAssignmentDto> list(String clientCode) {
        SysClientApplication client = clientApplicationRepository.findByClientCode(clientCode)
                .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + clientCode));
        return clientLayoutProfileRepository.findByClientApplicationIdAndActiveTrueOrderByDisplayOrderAscIdAsc(client.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    private SysClientApplication resolveClient(ClientLayoutAssignmentRequestDto request) {
        if (request.getClientApplicationId() != null) {
            return clientApplicationRepository.findById(request.getClientApplicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + request.getClientApplicationId()));
        }
        return clientApplicationRepository.findByClientCode(request.getClientCode())
                .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + request.getClientCode()));
    }

    private SysLayoutProfile resolveProfile(ClientLayoutAssignmentRequestDto request) {
        if (request.getLayoutProfileId() != null) {
            return layoutProfileRepository.findById(request.getLayoutProfileId())
                    .orElseThrow(() -> new IllegalArgumentException("Layout profile not found: " + request.getLayoutProfileId()));
        }
        return layoutProfileRepository.findByProfileCode(request.getProfileCode())
                .orElseThrow(() -> new IllegalArgumentException("Layout profile not found: " + request.getProfileCode()));
    }

    private ClientLayoutAssignmentDto toDto(SysClientLayoutProfile assignment) {
        return ClientLayoutAssignmentDto.builder()
                .id(assignment.getId())
                .clientApplicationId(assignment.getClientApplication().getId())
                .clientCode(assignment.getClientApplication().getClientCode())
                .layoutProfileId(assignment.getLayoutProfile().getId())
                .profileCode(assignment.getLayoutProfile().getProfileCode())
                .assignmentScope(assignment.getAssignmentScope())
                .roleCode(assignment.getRoleCode())
                .privilegeCode(assignment.getPrivilegeCode())
                .deviceTarget(assignment.getDeviceTarget())
                .moduleCode(assignment.getModuleCode())
                .defaultProfile(assignment.isDefaultProfile())
                .selectable(assignment.isSelectable())
                .displayOrder(assignment.getDisplayOrder())
                .active(assignment.isActive())
                .build();
    }
}
