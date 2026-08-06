package com.nexacore.systemmodule.workflow.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowHistoryDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowHistoryService;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowRuntimeService;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedApi;
import com.nexacore.systemmodule.accesscontrol.security.ApiDataScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/workflow")
@RequiredArgsConstructor
public class WorkflowRuntimeController {

    private final WorkflowRuntimeService workflowRuntimeService;
    private final WorkflowTaskService workflowTaskService;
    private final WorkflowHistoryService workflowHistoryService;

    @PostMapping("/start")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<WorkflowInstanceDto> start(@RequestBody WorkflowStartRequestDto request) {
        return ApiResponse.successCode(
                workflowRuntimeService.startWorkflow(request),
                "system.workflow.instance.started",
                "Workflow instance started"
        );
    }

    @PostMapping("/task/list")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<List<WorkflowTaskSummaryDto>> listTasks(@RequestBody WorkflowTaskSearchRequestDto request) {
        return ApiResponse.successCode(
                workflowTaskService.findUserTasks(request),
                "system.workflow.task.listed",
                "Workflow tasks listed"
        );
    }

    @PostMapping("/task/detail")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<WorkflowTaskDto> taskDetail(@RequestBody WorkflowTaskDetailRequestDto request) {
        return ApiResponse.successCode(
                workflowTaskService.getTask(request),
                "system.workflow.task.detail",
                "Workflow task detail loaded"
        );
    }

    @PostMapping("/task/action")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<WorkflowTaskDto> action(@RequestBody WorkflowActionRequestDto request) {
        return ApiResponse.successCode(
                workflowRuntimeService.completeTask(request),
                "system.workflow.task.action.completed",
                "Workflow task action completed"
        );
    }

    @PostMapping("/task/action/check")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<WorkflowDecisionResponseDto> checkAction(@RequestBody WorkflowActionRequestDto request) {
        return ApiResponse.successCode(
                workflowRuntimeService.canPerformAction(request),
                "system.workflow.task.action.checked",
                "Workflow task action checked"
        );
    }

    @PostMapping("/instance/detail")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<WorkflowInstanceDto> instanceDetail(@RequestBody WorkflowInstanceRequestDto request) {
        return ApiResponse.successCode(
                workflowRuntimeService.getInstance(request),
                "system.workflow.instance.detail",
                "Workflow instance detail loaded"
        );
    }

    @PostMapping("/instance/history")
    @AuthenticatedApi(dataScope = ApiDataScope.TENANT)
    public ApiResponse<List<WorkflowHistoryDto>> history(@RequestBody WorkflowInstanceRequestDto request) {
        return ApiResponse.successCode(
                workflowHistoryService.getHistory(request.workflowInstanceId()),
                "system.workflow.instance.history",
                "Workflow instance history loaded"
        );
    }
}
