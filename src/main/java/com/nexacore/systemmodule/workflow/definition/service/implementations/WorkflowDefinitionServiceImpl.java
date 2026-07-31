package com.nexacore.systemmodule.workflow.definition.service.implementations;

import com.nexacore.systemmodule.workflow.definition.dto.WorkflowAssignmentPolicyDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowPublishRequestDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowStepDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowTransitionDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowVersionDto;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowAssignmentPolicy;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowDefinition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowTransition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import com.nexacore.systemmodule.workflow.definition.enums.WorkflowDefinitionStatus;
import com.nexacore.systemmodule.workflow.definition.enums.WorkflowEngineType;
import com.nexacore.systemmodule.workflow.definition.enums.WorkflowStepType;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowAssignmentPolicyRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowDefinitionRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowStepRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowTransitionRepository;
import com.nexacore.systemmodule.workflow.definition.repository.WorkflowVersionRepository;
import com.nexacore.systemmodule.workflow.definition.service.interfaces.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowAssignmentPolicyRepository assignmentPolicyRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public WorkflowDefinitionDto save(WorkflowDefinitionDto request, String username) {
        SysWorkflowDefinition definition = resolveDefinition(request);
        definition.setWorkflowCode(requiredCode(request.workflowCode(), "workflowCode"));
        definition.setWorkflowName(requiredText(request.workflowName(), "workflowName"));
        definition.setModuleId(request.moduleId());
        definition.setSubmoduleId(request.submoduleId());
        definition.setFeatureId(request.featureId());
        definition.setSubjectType(requiredCode(request.subjectType(), "subjectType"));
        definition.setTenantId(request.tenantId());
        definition.setBusinessId(request.businessId());
        definition.setEngineType(request.engineType() == null ? WorkflowEngineType.LOCAL : request.engineType());
        definition.setActive(request.active() == null || request.active());
        definition = definitionRepository.save(definition);

        for (WorkflowVersionDto versionDto : nullSafe(request.versions())) {
            saveVersion(definition, versionDto);
        }

        return toDto(definition);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<WorkflowDefinitionDto> list(boolean activeOnly) {
        List<SysWorkflowDefinition> definitions = activeOnly
                ? definitionRepository.findByActiveTrueOrderByWorkflowCodeAsc()
                : definitionRepository.findAll();
        return definitions.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public WorkflowDefinitionDto publish(WorkflowPublishRequestDto request, String username) {
        SysWorkflowDefinition definition = findDefinition(request);
        SysWorkflowVersion version = versionRepository.findByWorkflowDefinitionAndVersionNumber(definition, request.versionNumber())
                .orElseThrow(() -> new IllegalArgumentException("Workflow version not found: " + request.versionNumber()));
        version.setStatus(WorkflowDefinitionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        version.setActive(true);
        versionRepository.save(version);
        return toDto(definition);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public WorkflowDefinitionDto retire(WorkflowPublishRequestDto request, String username) {
        SysWorkflowDefinition definition = findDefinition(request);
        SysWorkflowVersion version = versionRepository.findByWorkflowDefinitionAndVersionNumber(definition, request.versionNumber())
                .orElseThrow(() -> new IllegalArgumentException("Workflow version not found: " + request.versionNumber()));
        version.setStatus(WorkflowDefinitionStatus.RETIRED);
        version.setActive(false);
        versionRepository.save(version);
        return toDto(definition);
    }

    private void saveVersion(SysWorkflowDefinition definition, WorkflowVersionDto versionDto) {
        Integer versionNumber = versionDto.versionNumber() == null ? 1 : versionDto.versionNumber();
        SysWorkflowVersion version = versionRepository.findByWorkflowDefinitionAndVersionNumber(definition, versionNumber)
                .orElseGet(SysWorkflowVersion::new);
        if (version.getStatus() == WorkflowDefinitionStatus.PUBLISHED) {
            throw new IllegalStateException("Published workflow versions cannot be overwritten: " + versionNumber);
        }
        version.setWorkflowDefinition(definition);
        version.setVersionNumber(versionNumber);
        version.setStatus(versionDto.status() == null ? WorkflowDefinitionStatus.DRAFT : versionDto.status());
        version.setEffectiveFrom(versionDto.effectiveFrom());
        version.setEffectiveTo(versionDto.effectiveTo());
        version.setPublishedAt(versionDto.publishedAt());
        version.setActive(versionDto.active() == null || versionDto.active());
        version = versionRepository.save(version);

        transitionRepository.deleteByWorkflowVersion(version);
        assignmentPolicyRepository.deleteByWorkflowVersion(version);
        Map<String, SysWorkflowStep> stepsByCode = saveSteps(version, versionDto.steps());
        saveTransitions(version, stepsByCode, versionDto.transitions());
        saveAssignmentPolicies(version, stepsByCode, versionDto.assignmentPolicies());
    }

    private Map<String, SysWorkflowStep> saveSteps(SysWorkflowVersion version, List<WorkflowStepDto> stepDtos) {
        Map<String, SysWorkflowStep> stepsByCode = new LinkedHashMap<>();
        for (WorkflowStepDto stepDto : nullSafe(stepDtos)) {
            String stepCode = requiredCode(stepDto.stepCode(), "stepCode");
            SysWorkflowStep step = stepRepository.findByWorkflowVersionAndStepCode(version, stepCode).orElseGet(SysWorkflowStep::new);
            step.setWorkflowVersion(version);
            step.setStepCode(stepCode);
            step.setStepName(requiredText(stepDto.stepName(), "stepName"));
            step.setDisplayName(stepDto.displayName());
            step.setStepType(stepDto.stepType() == null ? WorkflowStepType.USER_TASK : stepDto.stepType());
            step.setTerminal(Boolean.TRUE.equals(stepDto.terminal()));
            step.setSortOrder(stepDto.sortOrder() == null ? 100 : stepDto.sortOrder());
            step.setActive(stepDto.active() == null || stepDto.active());
            stepsByCode.put(stepCode, stepRepository.save(step));
        }
        if (stepsByCode.isEmpty()) {
            throw new IllegalArgumentException("Workflow version must include at least one step");
        }
        return stepsByCode;
    }

    private void saveTransitions(SysWorkflowVersion version, Map<String, SysWorkflowStep> stepsByCode, List<WorkflowTransitionDto> transitionDtos) {
        for (WorkflowTransitionDto transitionDto : nullSafe(transitionDtos)) {
            SysWorkflowStep fromStep = requireStep(stepsByCode, transitionDto.fromStepCode());
            SysWorkflowStep toStep = requireStep(stepsByCode, transitionDto.toStepCode());
            transitionRepository.save(SysWorkflowTransition.builder()
                    .workflowVersion(version)
                    .fromStep(fromStep)
                    .toStep(toStep)
                    .actionCode(requiredCode(transitionDto.actionCode(), "actionCode"))
                    .actionName(requiredText(transitionDto.actionName(), "actionName"))
                    .requiredPrivilegeCode(blankToNull(transitionDto.requiredPrivilegeCode()))
                    .requiresComment(Boolean.TRUE.equals(transitionDto.requiresComment()))
                    .requiresAttachment(Boolean.TRUE.equals(transitionDto.requiresAttachment()))
                    .autoAssignNextTask(transitionDto.autoAssignNextTask() == null || transitionDto.autoAssignNextTask())
                    .active(transitionDto.active() == null || transitionDto.active())
                    .build());
        }
    }

    private void saveAssignmentPolicies(SysWorkflowVersion version, Map<String, SysWorkflowStep> stepsByCode, List<WorkflowAssignmentPolicyDto> policyDtos) {
        for (WorkflowAssignmentPolicyDto policyDto : nullSafe(policyDtos)) {
            assignmentPolicyRepository.save(SysWorkflowAssignmentPolicy.builder()
                    .workflowVersion(version)
                    .step(requireStep(stepsByCode, policyDto.stepCode()))
                    .policyType(policyDto.policyType())
                    .roleId(policyDto.roleId())
                    .userId(policyDto.userId())
                    .privilegeCode(blankToNull(policyDto.privilegeCode()))
                    .branchScoped(Boolean.TRUE.equals(policyDto.branchScoped()))
                    .businessScoped(Boolean.TRUE.equals(policyDto.businessScoped()))
                    .expressionKey(blankToNull(policyDto.expressionKey()))
                    .active(policyDto.active() == null || policyDto.active())
                    .build());
        }
    }

    private SysWorkflowDefinition resolveDefinition(WorkflowDefinitionDto request) {
        if (request.id() != null) {
            return definitionRepository.findById(request.id()).orElseGet(SysWorkflowDefinition::new);
        }
        return definitionRepository.findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(
                requiredCode(request.workflowCode(), "workflowCode"),
                requiredCode(request.subjectType(), "subjectType"),
                request.tenantId(),
                request.businessId()
        ).orElseGet(SysWorkflowDefinition::new);
    }

    private SysWorkflowDefinition findDefinition(WorkflowPublishRequestDto request) {
        return definitionRepository.findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(
                requiredCode(request.workflowCode(), "workflowCode"),
                requiredCode(request.subjectType(), "subjectType"),
                request.tenantId(),
                request.businessId()
        ).orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + request.workflowCode()));
    }

    private WorkflowDefinitionDto toDto(SysWorkflowDefinition definition) {
        return WorkflowDefinitionDto.builder()
                .id(definition.getId())
                .workflowCode(definition.getWorkflowCode())
                .workflowName(definition.getWorkflowName())
                .moduleId(definition.getModuleId())
                .submoduleId(definition.getSubmoduleId())
                .featureId(definition.getFeatureId())
                .subjectType(definition.getSubjectType())
                .tenantId(definition.getTenantId())
                .businessId(definition.getBusinessId())
                .engineType(definition.getEngineType())
                .active(definition.isActive())
                .versions(versionRepository.findByWorkflowDefinitionOrderByVersionNumberDesc(definition).stream()
                        .map(this::toVersionDto)
                        .toList())
                .build();
    }

    private WorkflowVersionDto toVersionDto(SysWorkflowVersion version) {
        List<SysWorkflowStep> steps = stepRepository.findByWorkflowVersionAndActiveTrueOrderBySortOrderAscIdAsc(version);
        return WorkflowVersionDto.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .effectiveFrom(version.getEffectiveFrom())
                .effectiveTo(version.getEffectiveTo())
                .publishedAt(version.getPublishedAt())
                .active(version.isActive())
                .steps(steps.stream().map(this::toStepDto).toList())
                .transitions(steps.stream()
                        .flatMap(step -> transitionRepository.findByWorkflowVersionAndFromStepAndActiveTrue(version, step).stream())
                        .map(this::toTransitionDto)
                        .toList())
                .assignmentPolicies(steps.stream()
                        .flatMap(step -> assignmentPolicyRepository.findByWorkflowVersionAndStepAndActiveTrue(version, step).stream())
                        .map(this::toPolicyDto)
                        .toList())
                .build();
    }

    private WorkflowStepDto toStepDto(SysWorkflowStep step) {
        return WorkflowStepDto.builder()
                .id(step.getId())
                .stepCode(step.getStepCode())
                .stepName(step.getStepName())
                .displayName(step.getDisplayName())
                .stepType(step.getStepType())
                .terminal(step.isTerminal())
                .sortOrder(step.getSortOrder())
                .active(step.isActive())
                .build();
    }

    private WorkflowTransitionDto toTransitionDto(SysWorkflowTransition transition) {
        return WorkflowTransitionDto.builder()
                .id(transition.getId())
                .fromStepCode(transition.getFromStep().getStepCode())
                .toStepCode(transition.getToStep().getStepCode())
                .actionCode(transition.getActionCode())
                .actionName(transition.getActionName())
                .requiredPrivilegeCode(transition.getRequiredPrivilegeCode())
                .requiresComment(transition.isRequiresComment())
                .requiresAttachment(transition.isRequiresAttachment())
                .autoAssignNextTask(transition.isAutoAssignNextTask())
                .active(transition.isActive())
                .build();
    }

    private WorkflowAssignmentPolicyDto toPolicyDto(SysWorkflowAssignmentPolicy policy) {
        return WorkflowAssignmentPolicyDto.builder()
                .id(policy.getId())
                .stepCode(policy.getStep().getStepCode())
                .policyType(policy.getPolicyType())
                .roleId(policy.getRoleId())
                .userId(policy.getUserId())
                .privilegeCode(policy.getPrivilegeCode())
                .branchScoped(policy.isBranchScoped())
                .businessScoped(policy.isBusinessScoped())
                .expressionKey(policy.getExpressionKey())
                .active(policy.isActive())
                .build();
    }

    private SysWorkflowStep requireStep(Map<String, SysWorkflowStep> stepsByCode, String code) {
        SysWorkflowStep step = stepsByCode.get(requiredCode(code, "stepCode"));
        if (step == null) {
            throw new IllegalArgumentException("Workflow step not found in version: " + code);
        }
        return step;
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String requiredCode(String value, String fieldName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized.toUpperCase();
    }

    private String requiredText(String value, String fieldName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
