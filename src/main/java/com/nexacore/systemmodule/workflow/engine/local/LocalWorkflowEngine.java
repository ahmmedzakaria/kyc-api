package com.nexacore.systemmodule.workflow.engine.local;

import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceListRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowAssignmentPolicy;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowDefinition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowTransition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import com.nexacore.systemmodule.workflow.definition.enums.WorkflowDefinitionStatus;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowAssignmentPolicyRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowDefinitionRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowStepRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowTransitionRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowVersionRepository;
import com.nexacore.systemmodule.workflow.engine.interfaces.WorkflowEngine;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowHistory;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowInstance;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowTask;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowHistoryEventType;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowInstanceStatus;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
import com.nexacore.systemmodule.workflow.execution.repository.WorkflowHistoryRepository;
import com.nexacore.systemmodule.workflow.execution.repository.WorkflowInstanceRepository;
import com.nexacore.systemmodule.workflow.execution.repository.WorkflowTaskRepository;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "nexacore.workflow.engine", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalWorkflowEngine implements WorkflowEngine {

    private static final Set<WorkflowTaskStatus> ACTIVE_TASK_STATUSES = Set.of(WorkflowTaskStatus.OPEN, WorkflowTaskStatus.CLAIMED);

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowAssignmentPolicyRepository assignmentPolicyRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowHistoryRepository historyRepository;
    private final PrivilegeModuleGateway privilegeModuleGateway;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public WorkflowInstanceDto start(WorkflowStartRequestDto request) {
        UserScopeAssignment scope = dataScopeService.requireWritableScope(
                request.tenantId(), request.businessId(), request.branchId());
        SysWorkflowDefinition definition = findDefinition(
                request.workflowCode(), request.subjectType(), scope.tenantId(), scope.businessId());
        SysWorkflowVersion version = versionRepository.findFirstByWorkflowDefinitionAndStatusAndActiveTrueOrderByVersionNumberDesc(
                definition,
                WorkflowDefinitionStatus.PUBLISHED
        ).orElseThrow(() -> new IllegalStateException("Published workflow version not found: " + definition.getWorkflowCode()));
        SysWorkflowStep firstStep = stepRepository.findByWorkflowVersionAndActiveTrueOrderBySortOrderAscIdAsc(version).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow version has no active steps: " + version.getId()));

        SysWorkflowInstance instance = instanceRepository.save(SysWorkflowInstance.builder()
                .workflowDefinition(definition)
                .workflowVersion(version)
                .workflowCode(definition.getWorkflowCode())
                .subjectType(request.subjectType())
                .subjectId(required(request.subjectId(), "subjectId"))
                .tenantId(scope.tenantId())
                .businessId(scope.businessId())
                .branchId(scope.branchId())
                .requesterUserId(request.requesterUserId())
                .currentStep(firstStep)
                .status(firstStep.isTerminal() ? WorkflowInstanceStatus.COMPLETED : WorkflowInstanceStatus.RUNNING)
                .completedAt(firstStep.isTerminal() ? LocalDateTime.now() : null)
                .build());
        history(instance, null, WorkflowHistoryEventType.INSTANCE_STARTED, null, null, firstStep, request.requesterUserId(), request.username(), null, "system.workflow.instance.started", request.safeContextJson());
        if (!firstStep.isTerminal()) {
            createTask(instance, firstStep);
        }
        return toInstanceDto(instance);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request) {
        SysWorkflowTask task = findTask(request);
        SysWorkflowTransition transition = findTransition(task, request.actionCode());
        List<String> availableActions = availableActions(task);
        if (!isAssignedToActor(task, request)) {
            return decision(false, "system.workflow.task.not_assigned", "Workflow task is not assigned to this actor", availableActions);
        }
        if (transition.isRequiresComment() && (request.comment() == null || request.comment().isBlank())) {
            return decision(false, "system.workflow.comment.required", "Workflow action requires a comment", availableActions);
        }
        if (!hasRequiredPrivilege(transition, request)) {
            return decision(false, "system.workflow.privilege.denied", "Required workflow action privilege is missing", availableActions);
        }
        return decision(true, "system.workflow.action.allowed", "Workflow action is allowed", availableActions);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public WorkflowTaskDto completeTask(WorkflowActionRequestDto request) {
        WorkflowDecisionResponseDto decision = canPerformAction(request);
        if (!decision.allowed()) {
            throw new IllegalStateException(decision.message());
        }
        SysWorkflowTask task = findTask(request);
        SysWorkflowTransition transition = findTransition(task, request.actionCode());
        SysWorkflowInstance instance = task.getWorkflowInstance();
        SysWorkflowStep fromStep = task.getStep();
        SysWorkflowStep toStep = transition.getToStep();

        task.setStatus(WorkflowTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setClaimedByUserId(request.actorUserId());
        taskRepository.save(task);
        history(instance, task, WorkflowHistoryEventType.TASK_COMPLETED, transition.getActionCode(), fromStep, toStep, request.actorUserId(), request.username(), request.comment(), "system.workflow.task.completed", request.safeContextJson());

        instance.setCurrentStep(toStep);
        if (toStep.isTerminal()) {
            instance.setStatus(WorkflowInstanceStatus.COMPLETED);
            instance.setCompletedAt(LocalDateTime.now());
            history(instance, task, WorkflowHistoryEventType.INSTANCE_COMPLETED, transition.getActionCode(), fromStep, toStep, request.actorUserId(), request.username(), request.comment(), "system.workflow.instance.completed", request.safeContextJson());
        }
        instanceRepository.save(instance);

        SysWorkflowTask nextTask = null;
        if (!toStep.isTerminal() && transition.isAutoAssignNextTask()) {
            nextTask = createTask(instance, toStep);
        }
        return toTaskDto(nextTask == null ? task : nextTask);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request) {
        Collection<WorkflowTaskStatus> statuses = request.statuses() == null || request.statuses().isEmpty()
                ? ACTIVE_TASK_STATUSES
                : request.statuses();
        Specification<SysWorkflowTask> activeStatus = (root, query, cb) -> root.get("status").in(statuses);
        return taskRepository.findAll(
                        dataScopeService.<SysWorkflowTask>restrictToCurrentScopes("tenantId", "businessId", "branchId")
                                .and(activeStatus),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(task -> isTaskVisibleToActor(task, request.userId(), request.roleIds(), request.privilegeCodes()))
                .map(this::toTaskSummaryDto)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public WorkflowTaskDto getTask(WorkflowTaskDetailRequestDto request) {
        SysWorkflowTask task = findScopedTask(request.workflowTaskId());
        return toTaskDto(task);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request) {
        SysWorkflowInstance instance = request.workflowInstanceId() != null
                ? findScopedInstance(request.workflowInstanceId())
                : findScopedInstanceBySubject(required(request.subjectType(), "subjectType"), required(request.subjectId(), "subjectId"));
        return toInstanceDto(instance);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<WorkflowInstanceDto> listInstances(WorkflowInstanceListRequestDto request) {
        Specification<SysWorkflowInstance> filters = (root, query, cb) -> cb.conjunction();
        if (request.workflowCode() != null && !request.workflowCode().isBlank()) {
            String code = request.workflowCode().toUpperCase();
            filters = filters.and((root, query, cb) -> cb.equal(root.get("workflowCode"), code));
        }
        if (request.subjectType() != null && !request.subjectType().isBlank()) {
            String type = request.subjectType().toUpperCase();
            filters = filters.and((root, query, cb) -> cb.equal(root.get("subjectType"), type));
        }
        if (request.status() != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("status"), request.status()));
        }
        if (request.businessId() != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("businessId"), request.businessId()));
        }
        if (request.branchId() != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("branchId"), request.branchId()));
        }
        return instanceRepository.findAll(
                        dataScopeService.<SysWorkflowInstance>restrictToCurrentScopes("tenantId", "businessId", "branchId").and(filters),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toInstanceDto)
                .toList();
    }

    private SysWorkflowDefinition findDefinition(String workflowCode, String subjectType, Long tenantId, Long businessId) {
        String code = required(workflowCode, "workflowCode").toUpperCase();
        String type = required(subjectType, "subjectType").toUpperCase();
        return definitionRepository.findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(code, type, tenantId, businessId)
                .or(() -> definitionRepository.findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(code, type, tenantId, null))
                .or(() -> definitionRepository.findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(code, type, null, null))
                .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + code));
    }

    private SysWorkflowTask createTask(SysWorkflowInstance instance, SysWorkflowStep step) {
        List<SysWorkflowAssignmentPolicy> policies = assignmentPolicyRepository.findByWorkflowVersionAndStepAndActiveTrue(instance.getWorkflowVersion(), step);
        SysWorkflowTask.SysWorkflowTaskBuilder builder = SysWorkflowTask.builder()
                .workflowInstance(instance)
                .step(step)
                .status(WorkflowTaskStatus.OPEN)
                .tenantId(instance.getTenantId())
                .businessId(instance.getBusinessId())
                .branchId(instance.getBranchId());
        policies.stream().findFirst().ifPresent(policy -> {
            builder.assignedUserId(policy.getUserId());
            builder.assignedRoleId(policy.getRoleId());
            builder.assignedPrivilegeCode(policy.getPrivilegeCode());
        });
        SysWorkflowTask task = taskRepository.save(builder.build());
        history(instance, task, WorkflowHistoryEventType.TASK_CREATED, null, null, step, null, null, null, "system.workflow.task.created", null);
        return task;
    }

    private SysWorkflowTask findTask(WorkflowActionRequestDto request) {
        if (request.workflowTaskId() != null) {
            SysWorkflowTask task = findScopedTask(request.workflowTaskId());
            if (!ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
                throw new IllegalStateException("Workflow task is not active");
            }
            return task;
        }
        SysWorkflowInstance instance = findScopedInstance(request.workflowInstanceId());
        return taskRepository.findFirstByWorkflowInstanceAndStatusInOrderByIdDesc(instance, ACTIVE_TASK_STATUSES)
                .orElseThrow(() -> new IllegalStateException("Workflow instance has no active task"));
    }

    private SysWorkflowTransition findTransition(SysWorkflowTask task, String actionCode) {
        return transitionRepository.findByWorkflowVersionAndFromStepAndActionCodeAndActiveTrue(
                task.getWorkflowInstance().getWorkflowVersion(),
                task.getStep(),
                required(actionCode, "actionCode").toUpperCase()
        ).orElseThrow(() -> new IllegalArgumentException("Workflow transition not found for action: " + actionCode));
    }

    private boolean hasRequiredPrivilege(SysWorkflowTransition transition, WorkflowActionRequestDto request) {
        String requiredPrivilegeCode = transition.getRequiredPrivilegeCode();
        if (requiredPrivilegeCode == null || requiredPrivilegeCode.isBlank()) {
            return true;
        }
        if (request.privilegeCodes() != null && request.privilegeCodes().contains(requiredPrivilegeCode)) {
            return true;
        }
        return request.username() != null && privilegeModuleGateway.hasPrivilege(request.username(), requiredPrivilegeCode);
    }

    private boolean isAssignedToActor(SysWorkflowTask task, WorkflowActionRequestDto request) {
        return isTaskVisibleToActor(task, request.actorUserId(), request.roleIds(), request.privilegeCodes());
    }

    private boolean isTaskVisibleToActor(SysWorkflowTask task, Long userId, Set<Long> roleIds, Set<String> privilegeCodes) {
        if (task.getAssignedUserId() == null && task.getAssignedRoleId() == null && task.getAssignedPrivilegeCode() == null) {
            return true;
        }
        if (task.getAssignedUserId() != null && task.getAssignedUserId().equals(userId)) {
            return true;
        }
        if (task.getAssignedRoleId() != null && roleIds != null && roleIds.contains(task.getAssignedRoleId())) {
            return true;
        }
        return task.getAssignedPrivilegeCode() != null && privilegeCodes != null && privilegeCodes.contains(task.getAssignedPrivilegeCode());
    }

    private SysWorkflowTask findScopedTask(Long taskId) {
        Specification<SysWorkflowTask> id = (root, query, cb) -> cb.equal(root.get("id"), taskId);
        return taskRepository.findOne(
                        dataScopeService.<SysWorkflowTask>restrictToCurrentScopes("tenantId", "businessId", "branchId").and(id))
                .orElseThrow(() -> new IllegalArgumentException("Workflow task not found: " + taskId));
    }

    private SysWorkflowInstance findScopedInstance(Long instanceId) {
        Specification<SysWorkflowInstance> id = (root, query, cb) -> cb.equal(root.get("id"), instanceId);
        return instanceRepository.findOne(
                        dataScopeService.<SysWorkflowInstance>restrictToCurrentScopes("tenantId", "businessId", "branchId").and(id))
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));
    }

    private SysWorkflowInstance findScopedInstanceBySubject(String subjectType, String subjectId) {
        Specification<SysWorkflowInstance> subject = (root, query, cb) -> cb.and(
                cb.equal(root.get("subjectType"), subjectType), cb.equal(root.get("subjectId"), subjectId));
        return instanceRepository.findAll(
                        dataScopeService.<SysWorkflowInstance>restrictToCurrentScopes("tenantId", "businessId", "branchId").and(subject),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found for subject"));
    }

    private List<String> availableActions(SysWorkflowTask task) {
        return transitionRepository.findByWorkflowVersionAndFromStepAndActiveTrue(task.getWorkflowInstance().getWorkflowVersion(), task.getStep())
                .stream()
                .map(SysWorkflowTransition::getActionCode)
                .toList();
    }

    private WorkflowDecisionResponseDto decision(boolean allowed, String code, String message, List<String> availableActions) {
        return WorkflowDecisionResponseDto.builder()
                .allowed(allowed)
                .messageCode(code)
                .message(message)
                .availableActionCodes(availableActions)
                .build();
    }

    private void history(SysWorkflowInstance instance,
                         SysWorkflowTask task,
                         WorkflowHistoryEventType eventType,
                         String actionCode,
                         SysWorkflowStep fromStep,
                         SysWorkflowStep toStep,
                         Long actorUserId,
                         String actorUsername,
                         String comment,
                         String messageCode,
                         String safeContextJson) {
        historyRepository.save(SysWorkflowHistory.builder()
                .workflowInstance(instance)
                .workflowTask(task)
                .eventType(eventType)
                .actionCode(actionCode)
                .fromStepCode(fromStep == null ? null : fromStep.getStepCode())
                .toStepCode(toStep == null ? null : toStep.getStepCode())
                .actorUserId(actorUserId)
                .actorUsername(actorUsername)
                .commentText(comment)
                .messageCode(messageCode)
                .safeContextJson(safeContextJson)
                .build());
    }

    private WorkflowInstanceDto toInstanceDto(SysWorkflowInstance instance) {
        SysWorkflowStep currentStep = instance.getCurrentStep();
        return WorkflowInstanceDto.builder()
                .id(instance.getId())
                .workflowCode(instance.getWorkflowCode())
                .subjectType(instance.getSubjectType())
                .subjectId(instance.getSubjectId())
                .tenantId(instance.getTenantId())
                .businessId(instance.getBusinessId())
                .branchId(instance.getBranchId())
                .requesterUserId(instance.getRequesterUserId())
                .currentStepCode(currentStep == null ? null : currentStep.getStepCode())
                .currentStepName(currentStep == null ? null : currentStep.getStepName())
                .status(instance.getStatus())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .build();
    }

    private WorkflowTaskDto toTaskDto(SysWorkflowTask task) {
        return WorkflowTaskDto.builder()
                .taskId(task.getId())
                .workflowInstanceId(task.getWorkflowInstance().getId())
                .stepCode(task.getStep().getStepCode())
                .stepName(task.getStep().getStepName())
                .status(task.getStatus())
                .assignedUserId(task.getAssignedUserId())
                .assignedRoleId(task.getAssignedRoleId())
                .assignedPrivilegeCode(task.getAssignedPrivilegeCode())
                .claimedByUserId(task.getClaimedByUserId())
                .claimedAt(task.getClaimedAt())
                .completedAt(task.getCompletedAt())
                .availableActionCodes(availableActions(task))
                .build();
    }

    private WorkflowTaskSummaryDto toTaskSummaryDto(SysWorkflowTask task) {
        SysWorkflowInstance instance = task.getWorkflowInstance();
        return WorkflowTaskSummaryDto.builder()
                .taskId(task.getId())
                .workflowInstanceId(instance.getId())
                .workflowCode(instance.getWorkflowCode())
                .subjectType(instance.getSubjectType())
                .subjectId(instance.getSubjectId())
                .stepCode(task.getStep().getStepCode())
                .stepName(task.getStep().getStepName())
                .status(task.getStatus())
                .assignedUserId(task.getAssignedUserId())
                .assignedRoleId(task.getAssignedRoleId())
                .assignedPrivilegeCode(task.getAssignedPrivilegeCode())
                .businessId(task.getBusinessId())
                .branchId(task.getBranchId())
                .createdAt(task.getCreatedAt())
                .dueAt(task.getDueAt())
                .build();
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
