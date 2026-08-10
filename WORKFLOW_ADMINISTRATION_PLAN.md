# Workflow Administration module (Definition + Runtime/Task) for system-frontend-21

Not yet implemented — this is the plan to review before starting any of it.

## Dependency on SYSTEM_MODULE_AUDIT_FINDINGS.md

A revalidation pass on [SYSTEM_MODULE_AUDIT_FINDINGS.md](./SYSTEM_MODULE_AUDIT_FINDINGS.md)
found, after this plan was drafted, that **workflow runtime authorization trusts
caller-supplied user IDs, usernames, role IDs, and privilege codes** when evaluating task
visibility and actions, and that workflow task/instance/history reads enforce
organizational scope but not consistently actor/object authorization within that scope
(see that document's Executive summary, items 1–2). This directly affects **Phase C**
below: `WorkflowTaskSearchRequestDto`'s `userId`/`username`/`roleIds`/`privilegeCodes`
are exactly the caller-supplied fields the finding describes — building a Task Inbox UI
against that endpoint as-is means the frontend becomes a convenient wrapper around an
already-exploitable gap (any authenticated caller could view or act on another user's
tasks by editing the request payload), not a new one it introduces. **Phase C should not
ship before that backend authorization gap is fixed** — Phase B (Definition
Administration) has no such dependency and can proceed independently. Separately,
[SYSTEM_MODULE_RESOLUTION_PLAN.md](./SYSTEM_MODULE_RESOLUTION_PLAN.md) is flagged in that
same document as predating these corrections and needing its own revision — this plan's
Phase B save flow should be re-checked against whatever `§1.3`
("update-by-ID paths silently fall back to insert," which explicitly names workflow
definition save as an affected path) resolves to, once that revision happens.

## Context

The backend `workflow` submodule (`com.nexacore.systemmodule.workflow`) is a real, fully
wired engine with no frontend consumer yet — `system-frontend-21/AGENTS.md` explicitly
scoped it out until now. Full research this session confirmed: the definition
(design-time) side has complete CRUD + list already (`/definition/save`,
`/definition/list`, `/definition/publish`, `/definition/retire`), correctly wired
privilege codes (`WORKFLOW_ADMINISTRATION_VIEW` = `11050100101`, `_MANAGE` = `11050100187`,
not orphaned), and no read/write asymmetry — unlike License/Layout at audit time. The
runtime side (start/task list/task detail/task action/action-check/instance
detail/instance history) is also fully wired in `LocalWorkflowEngine`, but **nothing in
the backend currently calls `start()`** — no other module is wired to trigger a workflow
yet — and there is **no "list all instances" endpoint**, only lookup-by-id or
lookup-by-subject. No nav-tree seed exists (next migration number: `V31`).

**Key finding that shapes this plan**: the closest existing precedent for nested-tree
admin UI, Layout Administration's Navigation Tree page
(`system-frontend-21/src/app/pages/layout/navigation-tree/navigation-tree.component.ts`),
saves **node-by-node through 5 separate per-level endpoints**. Workflow's
`/definition/save` is the opposite: it accepts and returns the **entire nested
`WorkflowDefinitionDto` tree in one atomic call** (`versions[]`, each version holding
`steps[]`/`transitions[]`/`assignmentPolicies[]` as three flat sibling lists,
cross-referenced by `stepCode` strings, not nested containment). No generic tree
component exists in `@nexacore/shared` either way — Navigation Tree is bespoke,
built from `DynamicListComponent` + `ModalComponent` + `DynamicFormComponent`. This plan
reuses that same "tab-per-collection + DynamicList + modal DynamicForm" *browsing/editing*
pattern, but changes the *save* semantics to match Workflow's actual contract: accumulate
edits to steps/transitions/policies in local component state, submit the whole assembled
`WorkflowDefinitionDto` in one call — a pattern with no existing precedent in this
codebase, built fresh here.

## Design

### Phase A — Backend: add `/instance/list` (small, additive)

**Problem**: Runtime/Task Administration was explicitly chosen in scope, but there's no
way to browse workflow instances today (only lookup by id or by subject) — an "Instance
Inspector" screen has nothing to browse without this.

**Approach**: same shape as this session's earlier License entitlement/key list
additions —
- `WorkflowInstanceRepository`: add `findByFilters(...)`-shaped query method (status,
  workflowCode, tenantId/businessId, requesterUserId, date range — all optional) or a
  `Specification`-based search, matching whatever query style
  `SysWorkflowInstance`'s existing repository already uses.
- New `WorkflowInstanceListRequestDto` (all fields optional) and reuse the existing
  `WorkflowInstanceDto` for the response list.
- New `WorkflowRuntimeService.listInstances(...)` + `WorkflowRuntimeController`
  `POST /instance/list`, gated by the same `WORKFLOW_ADMINISTRATION_VIEW` code the
  definition-list endpoint uses (runtime endpoints today use only `@AuthenticatedApi`
  with no coarse privilege code — this new admin-browsing endpoint should have one,
  since it's an admin capability, not a per-user task action). **Also apply whatever
  actor/object authorization fix comes out of the dependency noted above** to this new
  endpoint from the start, rather than adding it unauthorized and needing a follow-up.

**Blast radius**: purely additive, no existing endpoint changes.

### Phase B — Frontend: Workflow Definition Administration

Route `/workflow`, `WorkflowListComponent`, tabs `'definitions' | 'tasks' | 'instances'`
(mirrors License's `activeTab` signal pattern exactly —
`system-frontend-21/src/app/pages/license/license-list.component.ts`).

**Definitions tab**: `DynamicListComponent` of `WorkflowDefinitionDto` rows
(workflowCode, workflowName, subjectType, engineType, active, latest-version status
badge). Row action "Edit" swaps the tab's content (in-page state, not a new route) into
a **Definition Builder** view — `WorkflowDefinitionBuilderComponent` (or inline in
`WorkflowListComponent`, decide during implementation based on resulting file size):

- Header `DynamicForm` for the definition's own fields (workflowCode, workflowName,
  moduleId/submoduleId/featureId, subjectType, tenantId, businessId, engineType, active).
- **Version handling, scoped deliberately narrow**: show the latest `DRAFT` version (if
  any) as the actively-edited version; other versions (`PUBLISHED`/`RETIRED`) listed
  read-only for history. "New Draft Version" creates a fresh in-memory version scaffold
  (`versionNumber = max(existing) + 1`, `status: DRAFT`) if no draft exists yet.
  Publish/Retire act on the current draft via the existing
  `/definition/publish`/`/definition/retire` endpoints. This avoids a full separate
  versions-CRUD UI while still covering the real lifecycle the backend supports.
- **Steps/Transitions/Assignment Policies**: three `DynamicListComponent`s (own tabs or
  stacked sections within the builder), each backed by a **local signal array** (not an
  immediate backend call) for the version being edited — `stepsSignal`,
  `transitionsSignal`, `policiesSignal`. Each has its own "New"/"Edit" modal
  `DynamicForm`, matching Navigation Tree's per-level editor pattern exactly. Field
  specifics:
  - **Step editor**: stepCode, stepName, displayName, stepType (dropdown:
    START/USER_TASK/SYSTEM_TASK/END), terminal (checkbox), sortOrder (number), active.
  - **Transition editor**: fromStepCode/toStepCode as **dropdowns sourced from the
    current version's own `stepsSignal`** (by `stepCode`) — same "dropdown of sibling
    rows" pattern Navigation Tree uses for `parentId` — not free-text, to prevent typos
    breaking the code-reference chain. actionCode, actionName, requiredPrivilegeCode
    (plain text — no privilege picker endpoint confirmed available), requiresComment,
    requiresAttachment, autoAssignNextTask, active.
  - **Assignment Policy editor**: stepCode dropdown (same source), policyType (dropdown
    over the 7 enum values), roleId/userId (plain number inputs — no role/user picker
    endpoint confirmed available, same pragmatic call already made for License
    entitlements' moduleId/submoduleId/featureId fields), privilegeCode (text),
    branchScoped/businessScoped (checkboxes), expressionKey (text), active.
- **Save**: one "Save Definition" button assembles the whole `WorkflowDefinitionDto`
  (definition header fields + `[currentVersion-with-local-steps/transitions/policies,
  ...other read-only versions]`) and calls `workflowService.saveDefinition(dto)` once —
  the atomic-save semantics `/definition/save` actually expects, not per-node saves.

### Phase C — Frontend: Runtime/Task Administration (blocked — see Dependency section above)

**Tasks tab**: a task inbox. `workflowService.listTasks(...)` calling `/task/list` with
a `WorkflowTaskSearchRequestDto` built from the current session's username (via
whatever `AuthService`/`ApplicationContextService` in `@nexacore/platform` already
exposes for the logged-in user — reuse, don't reinvent) — **explicit scope limitation**:
`roleIds`/`privilegeCodes` are left empty for v1 since no confirmed endpoint exists in
this app to fetch the current user's role/privilege IDs as a list; this means role- or
privilege-assigned tasks won't surface here yet, only tasks directly assigned to the
user or unassigned (per `LocalWorkflowEngine.isTaskVisibleToActor`'s own visibility
rule) — flagged in `MODULES.md` as a known v1 gap, not silently dropped. Row click opens
a detail view: `WorkflowTaskDto` fields + `/task/action/check` to determine which
actions are currently allowed (drives which action buttons render), then `/task/action`
to execute, with a comment field shown only when the chosen transition's
`requiresComment` is true (mirrors DynamicForm's "no conditional field visibility"
constraint already documented elsewhere in this session — handle it at the component
level, not by inventing conditional FieldConfig support).

**Instances tab**: `DynamicListComponent` over the new `/instance/list` (Phase A),
columns: workflowCode, subjectType/subjectId, status badge, requesterUserId, startedAt,
completedAt. Row click → detail panel calling `/instance/detail` +
`/instance/history` (a simple timeline/list of `WorkflowHistoryDto` entries).

### Phase D — Scaffolding (routing, api-endpoints, service, model, nav seed, docs)

Follow the License module's established shape exactly, confirmed file-by-file this
session:
- `app.routes.ts`: add `{ path: 'workflow', loadComponent: () => import(...).then(m =>
  m.WorkflowListComponent), data: { public: true } }`.
- `core/api/api-endpoints.ts`: new `// Workflow` banner block,
  `WORKFLOW_DEFINITION_{SAVE,LIST,PUBLISH,RETIRE}`, `WORKFLOW_TASK_{LIST,DETAIL,ACTION,ACTION_CHECK}`,
  `WORKFLOW_INSTANCE_{DETAIL,HISTORY,LIST}`, `WORKFLOW_START` — mapped 1:1 to the
  backend paths confirmed this session.
- `core/services/workflow.service.ts`: one method per endpoint, `ApiService.post<T>`
  one-liners, matching `license.service.ts`'s exact shape.
- `pages/workflow/workflow.model.ts`: DTOs mirroring the backend 1:1 (all 5 definition
  DTOs + the runtime DTOs), enum union types, `DropdownOption[]` + `FieldConfig[]`
  exports for every editor listed above, following `license.model.ts`'s structure
  (mirrors-1:1 comment, date helpers only if any date fields need them — check
  `effectiveFrom`/`effectiveTo`/`dueAt` for the same `DynamicForm` date-string
  footgun already documented in `MODULES.md`).
- `db/migration/system/V31__seed_workflow_navigation.sql`: same idempotent
  `WHERE NOT EXISTS` two-insert shape as `V29__seed_license_navigation.sql` — a
  `WORKFLOW_ADMINISTRATION` feature (own group or under `ACCESS_CONTROL`, decide
  during implementation to match how License was grouped) linked to privilege
  `11050100101`.
- `pages/dashboard/dashboard.component.ts`: add a 5th stat card (cosmetic only, per
  confirmed convention — dashboard cards carry no navigation).
- `MODULES.md`: new "Phase 5 — Workflow Administration" section matching the
  established per-module documentation shape (routes, endpoints, DTO shapes, footguns
  found), plus a new row in the nav-tree-seeding table for `V31`, plus the explicit
  "Explicitly out of scope" callouts already established as this doc's convention:
  no role/privilege-based task visibility in v1, no `ModuleWorkflowProvider`
  implementations exist anywhere yet so `/provider/sync` will always no-op, no cancel
  endpoint exists so instances can't be cancelled from the UI.

### Verification

1. `mvn -q compile -DskipTests -o -f backend/pom.xml` — confirms Phase A's new
   repository/service/controller/DTO compile clean.
2. Ask the user to restart the backend so `V31` applies and `/instance/list` is live.
3. `cd frontend-libs-21 && npx ng build shared` — sanity check, no library changes
   expected in this module.
4. `cd system-frontend-21 && npx ng build` — catches type errors across the new
   list/service/model/routes.
5. Live browser check (`system_admin` login): navigate via sidebar (seeded by `V31`) to
   `/workflow`; create a Definition with a Basic Info + at least one Step, one
   Transition between two steps, one Assignment Policy; Save; confirm it round-trips
   correctly in the Definitions list and re-opening the builder shows the same
   steps/transitions/policies. Publish the draft version, confirm status badge updates.
   Switch to Tasks tab (expect empty — nothing triggers workflows yet, this is
   expected per Phase C's documented limitation). Switch to Instances tab (expect
   empty for the same reason) — confirm the list renders its empty state cleanly
   rather than erroring, since this is the realistic day-one state of this module.
