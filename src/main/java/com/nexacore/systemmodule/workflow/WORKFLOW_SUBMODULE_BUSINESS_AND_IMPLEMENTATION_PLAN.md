# Workflow Submodule Business And Implementation Plan

## Purpose

The workflow submodule will extend `systemmodule` with platform-owned configurable workflow management for approval, review, verification, send-back, rejection, escalation, and state-transition processes across business modules.

The first implementation will use NexaCore-owned workflow tables and services. A later implementation may use Flowable behind the same service contracts when a customer or business process requires BPMN, CMMN, DMN, timers, parallel gateways, or external workflow tooling.

This plan is written for the current backend project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Target module: `com.nexacore.systemmodule.workflow`
- Existing privilege package: `com.nexacore.systemmodule.privilege`
- Existing privilege provider pattern: `ModulePrivilegeProvider`
- Existing gateway package: `gatewaymodule`
- API response wrappers and common DTOs: `commonmodule`
- API access/error/audit logging: `logmodule`
- Flyway migration location: `src/main/resources/db/migration/system`

## Business Goals

- Provide a centralized workflow configuration service for KYC, POS, GIS, Auth, and future business modules.
- Support configurable approval chains without duplicating workflow engines inside business modules.
- Allow each business module to publish workflow definitions for its own features.
- Keep user authorization separate from workflow state decisions.
- Allow runtime workflow execution against generic business subjects such as `KYC_PERSON`, `POS_SALE`, `LICENSE_SUBSCRIPTION`, or `AUTH_USER_APPROVAL`.
- Support future replacement or augmentation with Flowable without changing business-module service code.
- Preserve a full workflow history for audit, compliance, troubleshooting, and reporting.
- Avoid logging sensitive payloads or storing full business records inside workflow tables.

## Ownership Boundary

Workflow management belongs in `systemmodule` because it is platform governance and orchestration, not a single business-domain behavior.

```text
systemmodule/workflow owns:
- workflow definitions
- workflow versions
- workflow states/steps
- workflow transitions/actions
- workflow assignment rules
- workflow execution instances
- workflow tasks
- workflow history
- workflow engine abstraction
- workflow administration APIs
```

Business modules own their domain records, validation rules, persistence, and final state changes.

```text
kycmodule owns:
- KycPerson
- KYC validation
- KYC document rules
- KYC approval side effects

systemmodule/workflow owns:
- the fact that a KYC person approval process is at Branch Review
- who may act on the current workflow task
- which transitions are available from the current step
- workflow history and task assignment state
```

Business modules must communicate with workflow through a module gateway or public workflow service interface. They must not depend directly on a concrete local workflow engine, Flowable APIs, workflow repositories, or workflow entities.

Recommended dependency direction:

```text
kycmodule / posmodule / gismodule / authmodule
  -> gatewaymodule.workflow.service.interfaces.WorkflowModuleGateway
  -> systemmodule.workflow.api.SystemWorkflowModuleGateway
  -> WorkflowRuntimeService
  -> WorkflowEngine
  -> sys_workflow_* tables or Flowable
```

## Core Design Decision

NexaCore should own the workflow contract even if Flowable is added later.

```text
Business modules call NexaCore workflow APIs.
NexaCore workflow APIs call a selected WorkflowEngine implementation.
The selected implementation may be local-table based or Flowable based.
```

Do not expose Flowable classes in business modules, controller DTOs, gateway DTOs, or shared workflow contracts.

Good:

```text
WorkflowRuntimeService.startWorkflow(...)
WorkflowRuntimeService.completeTask(...)
WorkflowRuntimeService.getAvailableActions(...)
```

Avoid:

```text
RuntimeService.startProcessInstanceByKey(...)
TaskService.complete(...)
ProcessInstance
Task
```

## Relationship With Privilege

Privilege and workflow are separate decisions.

```text
Privilege answers:
Can this user generally perform APPROVE on KYC Person?

Workflow answers:
Can this user approve this specific KYC Person at this current workflow step?

Business module answers:
Is this KYC Person valid to approve according to KYC business rules?
```

Final decision:

```text
allowed = privilege allowed + workflow task allowed + business validation passed
```

Workflow transitions should reference privilege codes from `systemmodule.privilege` when an action requires user authorization.

Example:

```text
KYC Person Branch Review -> APPROVE
required_privilege_code = 01010200107
```

The workflow service must call the existing privilege gateway/service to validate the authenticated user's effective privileges before allowing a task action.

## Relationship With Module Privilege Providers

The existing privilege system lets modules publish feature/action metadata through `ModulePrivilegeProvider`.

Workflow should follow the same provider pattern:

```java
public interface ModuleWorkflowProvider {
    List<WorkflowDefinitionDto> getWorkflowDefinitions();
}
```

Each business module owns its default workflow definitions:

```text
kycmodule/person/workflow/KycPersonWorkflowProvider.java
posmodule/workflow/PosWorkflowProvider.java
systemmodule/license/workflow/LicenseWorkflowProvider.java
```

`systemmodule.workflow` imports those definitions into `sys_workflow_*` tables during startup or through an admin sync API.

## Proposed Package Structure

Add a new package under `systemmodule`:

```text
com.nexacore.systemmodule.workflow
├── api
├── controller
├── definition
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
├── execution
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
├── engine
│   ├── interfaces
│   ├── local
│   └── flowable
├── provider
├── policy
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
└── service
    ├── interfaces
    └── implementations
```

Recommended gateway structure:

```text
gatewaymodule/workflow/service/interfaces/WorkflowModuleGateway.java
gatewaymodule/workflow/dto/WorkflowStartRequestDto.java
gatewaymodule/workflow/dto/WorkflowActionRequestDto.java
gatewaymodule/workflow/dto/WorkflowDecisionResponseDto.java
gatewaymodule/workflow/dto/WorkflowTaskSummaryDto.java
```

Recommended system implementation:

```text
systemmodule/workflow/api/SystemWorkflowModuleGateway.java
systemmodule/workflow/service/interfaces/WorkflowDefinitionService.java
systemmodule/workflow/service/interfaces/WorkflowRuntimeService.java
systemmodule/workflow/service/interfaces/WorkflowTaskService.java
systemmodule/workflow/service/interfaces/WorkflowHistoryService.java
systemmodule/workflow/engine/interfaces/WorkflowEngine.java
systemmodule/workflow/engine/local/LocalWorkflowEngine.java
systemmodule/workflow/engine/flowable/FlowableWorkflowEngine.java
```

## Engine Abstraction

The workflow engine interface should contain the stable behavior business modules need.

Suggested interface:

```java
public interface WorkflowEngine {
    WorkflowInstanceDto start(WorkflowStartRequestDto request);
    WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request);
    WorkflowTaskDto completeTask(WorkflowActionRequestDto request);
    List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request);
    WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request);
}
```

Initial implementation:

```text
LocalWorkflowEngine
```

Future implementation:

```text
FlowableWorkflowEngine
```

Engine selection should be configuration-driven:

```properties
nexacore.workflow.engine=local
```

Future option:

```properties
nexacore.workflow.engine=flowable
```

The local and Flowable engines should both return the same NexaCore DTOs.

## Core Concepts

### Workflow Definition

A reusable workflow template for a module feature.

Examples:

```text
KYC_PERSON_APPROVAL
POS_RETURN_APPROVAL
LICENSE_SUBSCRIPTION_APPROVAL
AUTH_USER_ACTIVATION_APPROVAL
```

### Workflow Version

A published definition version. Running workflow instances must stay tied to the definition version they started with.

Versioning prevents old instances from changing behavior when an administrator edits a workflow.

### Step

A workflow state where a record waits for action or reaches a terminal outcome.
Step codes and names are business-defined configuration for each workflow version.
Values such as `BRANCH_REVIEW` or `COMPLIANCE_REVIEW` are examples only, not fixed platform states.

Examples:

```text
DRAFT
SUBMITTED
BRANCH_REVIEW
COMPLIANCE_REVIEW
OPERATIONS_REVIEW
MANAGER_APPROVAL
APPROVED
REJECTED
CANCELLED
```

### Transition

An allowed action from one step to another.
Transitions are configured per workflow version, so each business can define its own path, action names, reviewer levels, and terminal outcomes.

Example seed path:

```text
SUBMIT: DRAFT -> SUBMITTED
APPROVE: BRANCH_REVIEW -> COMPLIANCE_REVIEW
FINAL_APPROVE: COMPLIANCE_REVIEW -> APPROVED
SEND_BACK: COMPLIANCE_REVIEW -> BRANCH_REVIEW
REJECT: BRANCH_REVIEW -> REJECTED
```

Alternative business-defined paths can replace those steps entirely:

```text
SUBMIT: DRAFT -> OPERATIONS_REVIEW
APPROVE: OPERATIONS_REVIEW -> MANAGER_APPROVAL
FINAL_APPROVE: MANAGER_APPROVAL -> APPROVED
SEND_BACK: MANAGER_APPROVAL -> OPERATIONS_REVIEW
```

### Assignment Policy

A rule that identifies who can claim or perform a task.

Supported initial policy types:

```text
ROLE
USER
PRIVILEGE
BRANCH_ROLE
BUSINESS_ROLE
REQUESTER_MANAGER
SYSTEM
```

### Workflow Instance

A runtime process for one business subject.

Example:

```text
workflow_code = KYC_PERSON_APPROVAL
subject_type = KYC_PERSON
subject_id = 123
current_step = BRANCH_REVIEW
status = RUNNING
```

### Workflow Task

The actionable item assigned to a user, role, branch, business, or system actor.

### Workflow History

An append-only record of workflow lifecycle events. It should include safe metadata, actor IDs, step changes, action codes, and message codes. It must not store full PII payloads, documents, tokens, credentials, or authorization headers.

## Database Plan

Add a Flyway migration under:

```text
src/main/resources/db/migration/system
```

Suggested migration name:

```text
V6__add_workflow_management.sql
```

Suggested tables:

| Table | Purpose |
| --- | --- |
| `sys_workflow_definitions` | Workflow catalog by module, submodule, feature, subject type |
| `sys_workflow_versions` | Versioned workflow definition metadata |
| `sys_workflow_steps` | Step/state catalog per workflow version |
| `sys_workflow_transitions` | Allowed action paths between steps |
| `sys_workflow_assignment_policies` | Actor/role/privilege/branch assignment rules |
| `sys_workflow_instances` | Runtime workflow process instances |
| `sys_workflow_tasks` | Runtime tasks for users/roles/branches/businesses |
| `sys_workflow_history` | Append-only lifecycle and action history |
| `sys_workflow_engine_mappings` | Optional mapping from NexaCore workflow/version to external engine deployment/process IDs |

Every table must include:

```text
created_by
updated_by
created_at
updated_at
```

Recommended key columns:

```text
sys_workflow_definitions
- id
- workflow_code
- workflow_name
- module_id
- submodule_id
- feature_id
- subject_type
- tenant_id
- business_id
- engine_type
- active

sys_workflow_versions
- id
- workflow_definition_id
- version_number
- status
- effective_from
- effective_to
- published_at
- active

sys_workflow_steps
- id
- workflow_version_id
- step_code
- step_name
- display_name
- step_type
- terminal
- sort_order
- active

sys_workflow_transitions
- id
- workflow_version_id
- from_step_id
- to_step_id
- action_code
- action_name
- required_privilege_id
- requires_comment
- requires_attachment
- auto_assign_next_task
- active

sys_workflow_assignment_policies
- id
- workflow_version_id
- step_id
- policy_type
- role_id
- user_id
- privilege_id
- branch_scoped
- business_scoped
- expression_key
- active

sys_workflow_instances
- id
- workflow_definition_id
- workflow_version_id
- workflow_code
- subject_type
- subject_id
- tenant_id
- business_id
- branch_id
- requester_user_id
- current_step_id
- status
- started_at
- completed_at
- cancelled_at
- engine_instance_id

sys_workflow_tasks
- id
- workflow_instance_id
- step_id
- status
- assigned_user_id
- assigned_role_id
- assigned_privilege_id
- tenant_id
- business_id
- branch_id
- claimed_by_user_id
- claimed_at
- due_at
- completed_at
- engine_task_id

sys_workflow_history
- id
- workflow_instance_id
- workflow_task_id
- event_type
- action_code
- from_step_code
- to_step_code
- actor_user_id
- comment_text
- message_code
- safe_context_json
```

Use application-level references for IDs owned by other databases, such as auth user IDs and role IDs. Do not create physical cross-database foreign keys from `system_db` to `auth_db`, `kyc_db`, or other module databases.

## Suggested Enums

```text
WorkflowEngineType
- LOCAL
- FLOWABLE

WorkflowDefinitionStatus
- DRAFT
- PUBLISHED
- RETIRED

WorkflowInstanceStatus
- RUNNING
- COMPLETED
- CANCELLED
- FAILED

WorkflowTaskStatus
- OPEN
- CLAIMED
- COMPLETED
- CANCELLED
- EXPIRED

WorkflowStepType
- START
- USER_TASK
- SYSTEM_TASK
- END

WorkflowAction
- SUBMIT
- APPROVE
- REJECT
- SEND_BACK
- CANCEL
- CLAIM
- RELEASE
- ESCALATE

WorkflowAssignmentPolicyType
- ROLE
- USER
- PRIVILEGE
- BRANCH_ROLE
- BUSINESS_ROLE
- REQUESTER_MANAGER
- SYSTEM
```

## Business Module Example: KYC Person Approval

KYC owns the person record and validation. Workflow owns the configurable approval path.
The default seed may use steps such as `BRANCH_REVIEW` and `COMPLIANCE_REVIEW`, but those values must be loaded from workflow configuration for the current tenant/business and workflow version.

```text
KYC request: submit person for approval
  -> KYC validates person profile, documents, business rules
  -> KYC asks WorkflowModuleGateway to start KYC_PERSON_APPROVAL for the current business context
  -> Workflow resolves the active business-specific workflow version and first configured step
  -> Workflow creates instance and first review task
  -> KYC stores workflow instance ID or workflow status reference if needed
```

Approval:

```text
KYC request: approve person
  -> API security validates JWT/client access
  -> KYC loads person and validates business state
  -> Workflow checks current task, assignment, branch/business scope, and required privilege
  -> Workflow completes current task and moves to the next step
  -> KYC applies domain side effects when workflow reaches APPROVED
```

KYC must not store workflow steps or transitions in KYC tables. It may store only the current high-level domain status and workflow instance reference when needed for search performance.

## API Plan

All APIs should use explicit DTOs and message codes.

Recommended admin APIs:

```text
POST /system/workflow/definition/save
POST /system/workflow/definition/list
POST /system/workflow/definition/publish
POST /system/workflow/definition/retire
POST /system/workflow/provider/sync
```

Recommended runtime APIs:

```text
POST /system/workflow/start
POST /system/workflow/task/list
POST /system/workflow/task/detail
POST /system/workflow/task/action
POST /system/workflow/instance/detail
POST /system/workflow/instance/history
```

Admin APIs require system setup privileges. Runtime APIs require the relevant workflow transition privilege and task assignment access.

## Message Codes

Avoid hard-coded user-facing text. Suggested message codes:

```text
workflow.definition.created
workflow.definition.updated
workflow.definition.published
workflow.definition.retired
workflow.instance.started
workflow.task.completed
workflow.task.not.assigned
workflow.task.invalid.state
workflow.transition.not.allowed
workflow.transition.privilege.denied
workflow.provider.synced
workflow.engine.unavailable
```

Add message bundle entries when implementing APIs.

## Local Engine Rules

The first implementation should be intentionally small and reliable.

Local engine must support:

- Start workflow instance.
- Create task for the initial actionable step.
- Resolve available actions from current step.
- Check required privilege code.
- Check task assignment policy.
- Complete task and move to next step.
- Create next task when the next step is not terminal.
- Mark instance completed when terminal step is reached.
- Write append-only history for every state change.

Local engine may defer:

- Parallel tasks.
- Timers.
- Complex expressions.
- BPMN import/export.
- DMN decision tables.
- External worker jobs.

Those deferred features are a better fit for Flowable later.

## Future Flowable Integration

Flowable should be added only behind:

```text
systemmodule.workflow.engine.interfaces.WorkflowEngine
```

Flowable integration package:

```text
com.nexacore.systemmodule.workflow.engine.flowable
```

Flowable-specific tables and deployment IDs should remain internal implementation details.

Mapping table:

```text
sys_workflow_engine_mappings
- workflow_definition_id
- workflow_version_id
- engine_type
- external_definition_key
- external_deployment_id
- external_version
```

Flowable implementation responsibilities:

- Deploy BPMN resources generated from or linked to NexaCore workflow definitions.
- Start Flowable process instances from NexaCore start requests.
- Map Flowable task IDs to NexaCore workflow task DTOs.
- Complete Flowable tasks from NexaCore action requests.
- Publish history back into `sys_workflow_history` or expose it through NexaCore DTOs.

Business modules must remain unchanged when switching from local to Flowable.

## Security Rules

- Enforce backend authorization even if frontend hides unavailable workflow actions.
- Validate current authenticated user, tenant, business, and branch context before task action.
- Never log full workflow request payloads when they contain PII or document metadata.
- Do not store raw OTP, token, password, card, Firebase credential, authorization header, or full PII payloads in workflow history.
- Use `safe_context_json` only for non-sensitive diagnostic metadata.
- Apply business and branch filtering for task lists.
- Use explicit response DTOs; do not return workflow entities directly from controllers.

## Audit And Logging

Workflow history and API/audit logs are related but separate.

```text
sys_workflow_history:
- domain workflow trail
- step/action/task changes
- actor IDs
- comments and safe metadata

logmodule:
- API access logs
- error logs
- technical audit logs
```

Important workflow actions should write workflow history. API failures and security events should continue to use `logmodule`.

## Implementation Phases

### Phase 1: Contracts And Local Runtime

- Add `gatewaymodule.workflow` DTOs and `WorkflowModuleGateway`.
- Add `systemmodule.workflow` package structure.
- Add workflow definition, version, step, transition, assignment, instance, task, and history entities.
- Add `V6__add_workflow_management.sql`.
- Add `WorkflowEngine` interface.
- Add `LocalWorkflowEngine`.
- Add `WorkflowRuntimeService`, `WorkflowTaskService`, and `WorkflowDefinitionService`.
- Add unit tests for start, available actions, complete task, terminal state, invalid transition, missing privilege, and assignment failure.

### Phase 2: Provider Sync And Admin APIs

- Add `ModuleWorkflowProvider`.
- Add provider sync service.
- Add admin APIs to save, list, publish, retire, and sync workflow definitions.
- Add message codes and localized fallbacks.
- Seed initial KYC Person Approval workflow through a KYC provider.
- Add setup privileges for workflow administration.

### Phase 3: Business Module Integration

- Integrate KYC Person approval with `WorkflowModuleGateway`.
- Keep KYC domain status separate from workflow runtime state.
- Add task list/search APIs with business and branch filters.
- Add workflow action endpoints or module-specific action endpoints that call workflow gateway.
- Add tests with mocked workflow gateway in KYC service tests.

### Phase 4: Advanced Local Features

- Add task claim/release.
- Add due dates and escalation metadata.
- Add comments and safe attachments metadata.
- Add workflow dashboard/report DTOs.
- Add system-task hook contracts for simple automatic transitions.

### Phase 5: Optional Flowable Engine

- Add Flowable dependencies only when needed.
- Add `FlowableWorkflowEngine`.
- Add engine mapping persistence.
- Add BPMN deployment/import strategy.
- Add compatibility tests proving business modules work through the same `WorkflowModuleGateway`.
- Keep local engine available for lightweight customers.

## Verification

For implementation work, run:

```bash
mvn test
```

For migration or configuration changes, also validate the relevant Flyway migration path and application startup when local databases are available.

If Docker, local databases, network access, or dependencies are unavailable, report that clearly.
