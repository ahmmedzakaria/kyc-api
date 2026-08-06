package com.nexacore.systemmodule.workflow.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionListRequestDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowPublishRequestDto;
import com.nexacore.systemmodule.workflow.definition.service.interfaces.WorkflowDefinitionService;
import com.nexacore.systemmodule.workflow.definition.service.interfaces.WorkflowProviderSyncService;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/workflow")
@RequiredArgsConstructor
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowProviderSyncService workflowProviderSyncService;

    @PostMapping("/definition/save")
    @PrivilegeApi("11050100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).WORKFLOW_ADMINISTRATION_MANAGE)")
    public ApiResponse<WorkflowDefinitionDto> save(@RequestBody WorkflowDefinitionDto request, Authentication authentication) {
        return ApiResponse.successCode(
                workflowDefinitionService.save(request, username(authentication)),
                "system.workflow.definition.saved",
                "Workflow definition saved"
        );
    }

    @PostMapping("/definition/list")
    @PrivilegeApi("11050100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).WORKFLOW_ADMINISTRATION_VIEW)")
    public ApiResponse<List<WorkflowDefinitionDto>> list(@RequestBody(required = false) WorkflowDefinitionListRequestDto request) {
        boolean activeOnly = request == null || request.activeOnly() == null || request.activeOnly();
        return ApiResponse.successCode(
                workflowDefinitionService.list(activeOnly),
                "system.workflow.definition.listed",
                "Workflow definitions listed"
        );
    }

    @PostMapping("/definition/publish")
    @PrivilegeApi("11050100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).WORKFLOW_ADMINISTRATION_MANAGE)")
    public ApiResponse<WorkflowDefinitionDto> publish(@RequestBody WorkflowPublishRequestDto request, Authentication authentication) {
        return ApiResponse.successCode(
                workflowDefinitionService.publish(request, username(authentication)),
                "system.workflow.definition.published",
                "Workflow definition published"
        );
    }

    @PostMapping("/definition/retire")
    @PrivilegeApi("11050100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).WORKFLOW_ADMINISTRATION_MANAGE)")
    public ApiResponse<WorkflowDefinitionDto> retire(@RequestBody WorkflowPublishRequestDto request, Authentication authentication) {
        return ApiResponse.successCode(
                workflowDefinitionService.retire(request, username(authentication)),
                "system.workflow.definition.retired",
                "Workflow definition retired"
        );
    }

    @PostMapping("/provider/sync")
    @PrivilegeApi("11050100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).WORKFLOW_ADMINISTRATION_MANAGE)")
    public ApiResponse<List<WorkflowDefinitionDto>> sync(Authentication authentication) {
        return ApiResponse.successCode(
                workflowProviderSyncService.sync(username(authentication)),
                "system.workflow.provider.synced",
                "Workflow providers synced"
        );
    }

    private String username(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
