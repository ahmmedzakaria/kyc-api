# System Module Audit — Findings

This report documents an inconsistency/gap audit of `com.nexacore.systemmodule`
(`backend/src/main/java/com/nexacore/systemmodule/`): the `accesscontrol`, `privilege`,
`layout`, `license`, and `workflow` submodules. It was produced while building the
`system-frontend-21` admin console (see `frontendApplications/system-frontend-21/MODULES.md`
for the frontend-side module reference) and covers the whole backend module, not just
what that console consumes.

Companion document: [SYSTEM_MODULE_RESOLUTION_PLAN.md](./SYSTEM_MODULE_RESOLUTION_PLAN.md)
— what to do about each finding below, in priority order.

> **Revalidation note:** A subsequent source-level review confirmed most of the original
> CRUD and DTO findings, corrected the failure mode of feature-privilege synchronization,
> and found higher-risk workflow authorization, access-control enforcement, and license
> integrity gaps. The companion resolution plan predates these corrections and must be
> revised before implementation.

## Executive summary

The highest-risk gaps are:

1. Workflow runtime authorization trusts caller-supplied user IDs, usernames, role IDs,
   and privilege codes when evaluating task visibility and actions.
2. Workflow task, instance, and history reads enforce organizational scope but do not
   consistently enforce actor/object authorization within that scope.
3. Access control defaults to `REPORT`, including under production profiles, so filter
   denials are logged but permitted unless deployment selects `ENFORCE`.
4. License `ADD_ON` entitlements can be stored but are unconditionally rejected by the
   decision engine, and entitlement target shapes are not validated.
5. Layout feature-privilege synchronization is not idempotent: database uniqueness turns
   repeated inserts into constraint failures rather than allowing duplicates.

Recommended order is workflow identity/object authorization, production enforcement,
license decision integrity, layout synchronization, client bootstrapping, then
administrative read/write parity.

This is a read-only audit — nothing here has been changed as part of writing it, except
where a finding is explicitly marked **(already fixed)** because the frontend work that
triggered its discovery included a backend fix at the time.

**Scope boundary relevant to [MULTI_TENANT_SAAS_PLAN.md](./MULTI_TENANT_SAAS_PLAN.md)**:
this audit evaluated the system as it actually runs today — a single-tenant deployment.
Two things that plan treats as real gaps are deliberately absent here because they're
harmless under that assumption and only become risks once multiple tenants share the
schema: License endpoints trusting a caller-supplied `tenantId`/`businessId` rather than
a server-resolved one (see that plan's Enforcement §3, Layer 2), and
`SysPrivClientApplicationTenant.tenantId` having no backing table to be a real foreign
key against (see that plan's §1, "Tenant identity," and
[SYSTEM_MODULE_RESOLUTION_PLAN.md](./SYSTEM_MODULE_RESOLUTION_PLAN.md) §3.1). Neither is
a finding here because neither is a defect against the system this audit actually
covers — flagged so their absence doesn't read as an oversight.

---

## 1. Data-loss / duplication risk

### 1.1 License entitlement save is insert-only with no identity match
`LicensePlanServiceImpl.savePlanEntitlement`, `LicenseSubscriptionServiceImpl.saveEntitlementOverride`.
`LicenseEntitlementRequestDto` has no `id` field and neither method looks up an existing
row by content before inserting. Every call — including an accidental double-submit or
a caller resaving the same entitlement — creates a new `sys_license_plan_entitlements`
/ `sys_license_entitlement_overrides` row. There is currently no way to update or
deactivate a previously-created entitlement at all; the only "delete" path is direct
DB access. This is worse than 1.2 below because there's no identity concept whatsoever,
not even an imperfect one.

### 1.2 Feature privilege sync is neither idempotent nor replace-capable
`LayoutNavigationServiceImpl.syncFeaturePrivileges` never clears or looks up existing
`sys_layout_feature_privileges` links before inserting. Migration `V6` creates the
unique index `uk_sys_layout_feature_privileges_feature_privilege` on
`(layout_feature_id, privilege_id)`, so resaving an unchanged privilege normally raises
a unique-constraint error and rolls back instead of persisting another duplicate row.

Removed privilege codes also remain linked, and an empty list returns early, so callers
cannot clear all links. This should be a transactional full-replace or set-difference
update, matching `LayoutUiPolicyServiceImpl`.

### 1.3 Update-by-ID paths silently fall back to insert
`LicensePlanServiceImpl.savePlan` and
`LicenseSubscriptionServiceImpl.assignSubscription` call `findById(id).orElseGet(new)`.
A request containing a nonexistent update ID is treated as a create, either inserting
an unexpected row or failing later on a unique business-code constraint. The same
fallback pattern appears in workflow definition, layout policy, and API registry save
paths and should be reviewed consistently. When an ID is supplied, absence must produce
a not-found error; only ID-absent requests should create or upsert by business code.

---

## 2. Bootstrapping / runtime gaps

### 2.1 System Admin has no durable permission bootstrap; WEB uses broad startup seeding
`V24__seed_system_admin_client_and_navigation.sql` registers the `SYSTEM_ADMIN_WEB`
client application row but never inserts into `sys_priv_client_api_permissions` or
`sys_priv_client_feature_permissions`. Checked across all 29 migrations: **no client,
including `WEB`/kyc-frontend, is ever granted these permissions by a migration** — the
only `INSERT INTO sys_priv_client_feature_permissions` statements (`V14`, `V15`) are
re-keying migrations against *existing* rows during a privilege-code realignment, not
fresh grants. `ClientAccessDecisionServiceImpl` reads directly from these two tables to
gate every non-public request (`decide()`).

The original claim that working `WEB` permissions must have been granted manually is
stale. `authmodule.startup.DataSeeder.seedDefaultWebClient` synchronizes the annotation
API inventory, assigns every active annotation-discovered non-public API to `WEB`, and
assigns the supplied privilege-code set on startup. This avoids a fresh-database gap
for `WEB`, but introduces four inconsistencies:

- `SYSTEM_ADMIN_WEB` and future clients still have no equivalent bootstrap.
- Access-control state is mutated from the Auth module rather than owned by System.
- The startup assignment is broader than least privilege.
- Full-replace startup seeding can restore permissions an administrator intentionally
  removed, making startup behavior compete with administrative configuration.

Choose one authoritative bootstrap mechanism before adding migrations. Do not seed the
same `WEB` assignments from both Flyway and `DataSeeder`.

This gap was masked for most of the `system-frontend-21` build by an unrelated frontend
bug (`ApiService.buildHeaders()` required both `clientCode` AND `apiKey` truthy before
sending `X-Client-Code`, so the client was never resolved server-side and
`ClientAccessDecisionServiceImpl.decide()` always hit its `clientApplication == null` →
`CLIENT_REQUIRED` → bypassed-for-browser-requests path). Fixing that frontend bug is
what surfaced this backend gap.

### 2.2 `routePolicies`/`uiPolicies` are seeded for KYC, not for System Admin
`V26__seed_person_route_and_ui_policies.sql` actively seeds both tables (`GLOBAL`,
`client_application_id NULL`) for the Person module, and its comment confirms this is
enforced by `routePrivilegeGuard`/`AuthorizedUiDirective` on the frontend — the
mechanism is real and in active use by `WEB`. It is specifically **not** seeded for any
`system-frontend-21` route, which is why every route in that app is currently marked
`data: { public: true }` at the Angular guard level (server-side `@PrivilegeApi`
enforcement is unaffected either way).

---

## 3. Design inconsistencies

### 3.0 Production access-control enforcement remains fail-open by default
`application.properties` defaults `access-control.enforcement-mode` to `REPORT`,
`require-client-for-browser` to `false`, and registry coverage to enabled. In `REPORT`,
client and user decisions that would deny a request are logged and allowed to continue.
Unregistered application APIs are denied only in `ENFORCE`.

`AccessControlProperties.validateProductionMode()` rejects `DISABLED` for `prod` and
`production`, but permits `REPORT`. Correct production authorization therefore depends
on external configuration being set perfectly. Administrative controllers have
method-level `@PreAuthorize` defense in depth, but ordinary `@AuthenticatedApi` runtime
controllers—especially workflow—do not all have equivalent method authorization.

Production profiles should reject every mode other than `ENFORCE`, with a startup test
covering the rule.

### 3.1 Privilege-code granularity split (two tiers, internally consistent)
`accesscontrol` controllers check 8 distinct fine-grained codes (`CLIENT_APPLICATION_VIEW`/
`MANAGE`, `CLIENT_CREDENTIAL_ROTATE`, `CLIENT_API_PERMISSION_ASSIGN`,
`CLIENT_FEATURE_PERMISSION_ASSIGN`, `CLIENT_TENANT_ASSIGN`, `API_REGISTRY_VIEW`/`MANAGE`/
`SYNCHRONIZE`). `layout`, `license`, `privilege`, and `workflow` each use exactly one
coarse VIEW/MANAGE pair for their entire submodule, centrally defined in
`privilege/bootstrap/BootstrapAdministrationPrivileges.java`. This reads as a
deliberate two-tier design (fine RBAC where external client integrations are involved,
coarse admin gates elsewhere) rather than drift — every controller's actual codes trace
back cleanly to one of the two patterns with no stragglers.

### 3.2 `LicensePrivilegeProvider` registers privilege codes no controller checks
`license/LicensePrivilegeProvider.java` registers 6 features (License Plan,
Subscription, Key, Activation, Usage, Audit) × up to 4 action codes each into the
privilege catalog. `LicenseController` never checks any of them — every endpoint uses
only the two umbrella `LICENSE_ADMINISTRATION_VIEW`/`MANAGE` codes from
`BootstrapAdministrationPrivileges`. No other submodule has an orphaned
`ModulePrivilegeProvider` like this; `BootstrapAdministrationPrivilegeProvider` (the
only other implementation) registers exactly the codes its controllers actually use.

### 3.3 Full-replace assignment endpoints with no matching read-back
`ClientPermissionServiceImpl`'s three assign methods (API permissions, feature
permissions, tenant assignments) and `PrivilegeServiceImpl`'s
`assignPrivilegesToRole`/`assignPrivilegesToUser` are all full-replace writes with no
corresponding getter anywhere — not even on `ClientApplicationDto`, and not for an
arbitrary target role/user (`getUserPrivilegeCodes` only covers the *currently
authenticated* caller). Any admin UI built against these has to track "what did we
already grant" state entirely client-side, and every save silently overwrites whatever
the UI didn't know about. This is the same shape of gap that `NavNodeDto` (§ below) and
the License entitlement/key list endpoints (already fixed this session) both were
before their fixes.

### 3.4 Response DTO round-trip / field-parity gaps
- **`LicenseSubscriptionResponseDto` drops `metadataJson`** even though
  `LicenseSubscriptionRequestDto.metadataJson` accepts it on write — `toResponse()`
  never maps it back, so a caller can set it but never read it.
- **`NavNodeDto` originally had no `id`/`parentId`/`active`/`displayOrder` (already
  fixed).** Before the fix, every `navigation/*/save` response and the nav-tree read
  endpoints omitted fields the *request* DTO required, making it impossible to resolve
  a newly-created node's id for use as a child's `parentId`, or to pre-fill an edit
  form's `active`/`displayOrder` without guessing. Fixed by widening `NavNodeDto` and
  `LayoutNavigationServiceImpl`'s builder methods this session.
- **`LayoutProfileRequestDto.profileCode`/`profileName` vs `LayoutProfileDto.code`/
  `name`** — same underlying fields, different names request-vs-response. Recurs in
  **Layout Navigation**: `LayoutNavigationNodeRequestDto.name` vs. `NavNodeDto.label`
  for the same entity field. Does not recur in License or Access Control, whose
  request/response DTOs use identical field names throughout.

### 3.5 Two navigation-tree read paths existed as one method (already fixed)
`getNavigationTree()` prunes any branch with no privilege-visible feature — correct for
the real app sidebar (its only caller before this session), wrong for an admin CRUD
screen, where a freshly-created empty `ModuleGroup`/`Module`/etc. must stay visible so
an admin can build out its children. Fixed by adding an unpruned
`getFullNavigationTree()` / `POST /navigation/tree/admin`, used only by
`system-frontend-21`'s Navigation Tree page. `LayoutController.java` still carries a
`// @Todo Need to Validate with "/navigation/tree"` comment on the new endpoint —
the original author flagged the same dual-consumer concern independently; it reads as
unresolved rather than stale.

### 3.6 Full-replace assignments have a lost-update race
The client and role/user assignment endpoints replace the whole stored set. Even after
adding the missing read-back endpoints from §3.3, two administrators can load the same
assignment, edit independently, and the later save silently overwrites the earlier one.
Use an assignment version/ETag or explicit add/remove commands if concurrent
administration is expected.

---

## 4. Minor

### 4.1 `DataScopeService`'s tenant-scoping `@Todo`
`DataScopeService.java` has an unresolved `@Todo` about simplifying tenant scoping — no
context in the surrounding code about what "simplifying" means; needs the original
author's input.

### 4.2 Primitive vs boxed `Boolean` inconsistency
`LicensePlanResponseDto`/`LicenseSubscriptionResponseDto` use primitive `boolean` for
`active`/`autoRenew` while their request counterparts use boxed `Boolean` — harmless,
purely stylistic.

### 4.3 Thin test coverage for `license`/`workflow`
Test coverage is thin for `license` (2 test files) and `workflow` (1) relative to
`accesscontrol` (17), `privilege` (4), `layout` (3) — proportionate to how recently
each was built, not necessarily a problem, but worth knowing before extending either.

---

## 5. License decision and data-integrity gaps

### 5.1 `ADD_ON` entitlements are writable but can never allow access
`LicenseEntitlementType.ADD_ON` is accepted by entitlement DTOs and persisted, but
`LicenseDecisionServiceImpl.matchesPlanEntitlement` and `matchesOverride` both return
`false` unconditionally for `ADD_ON`. An administrator can therefore configure an
entitlement that the decision engine will always deny. Implement a stable add-on
identifier and matching rule, or remove `ADD_ON` from writable values until supported.

### 5.2 Entitlement target shape is not validated
Plan and override saves copy all optional target fields without validating them against
`entitlementType`. The API currently permits, among other invalid combinations:

- `MODULE` without `moduleId`;
- `FEATURE` with unrelated module, feature, and API IDs;
- `LIMIT` without `limitCode` or `limitValue`;
- non-LIMIT rows retaining limit fields; and
- catalog IDs that exist individually but do not belong to the same hierarchy.

These rows are administratively valid-looking but never match, or match ambiguously.
The service must enforce exactly one target shape, validate hierarchy using loaded
entities, normalize irrelevant fields to null, and reject nonexistent references.

### 5.3 Subscription ownership permits ambiguous owner combinations
`assignSubscription` and migration `V5` require at least one of `tenantId`, `businessId`,
or `clientApplicationId`, but permit several simultaneously. The intended meaning—one
owner type versus a composite/hierarchical owner—is undocumented. A `businessId`
without `tenantId` also conflicts with the system's hierarchical scope rule. Define the
ownership model and enforce it identically in DTO validation, service logic, database
checks, and decision queries.

### 5.4 Inactive entitlements cannot be read back for reactivation
The administrative entitlement list methods query only `active=true`. Once update and
deactivation support is added, a deactivated row disappears and cannot be selected for
reactivation. Administrative lists need an `activeOnly` option or should return all
records by default.

---

## 6. Workflow submodule

The workflow module is not scaffolding: it is a real, wired engine with definition,
execution, task, history, provider, and local-engine packages. That makes the following
runtime authorization gaps immediately relevant even though no frontend currently
consumes the module.

### 6.1 Workflow authorization trusts caller-supplied actor identity and authority
`WorkflowActionRequestDto` accepts `actorUserId`, `username`, `roleIds`, and
`privilegeCodes` from the HTTP request. `LocalWorkflowEngine` uses those values directly
in `isAssignedToActor`, `isTaskVisibleToActor`, and `hasRequiredPrivilege`. An
authenticated caller can claim another user ID, arbitrary roles, or the transition's
required privilege code. Organizational scope limits which task can be reached, but it
does not prove assignment within that scope.

The same trust problem affects `/task/list` visibility and `/start` audit identity.
Build an internal actor context from Spring `Authentication`,
`AuthenticatedRequestContext`, and the Auth/Privilege gateways. HTTP callers should
supply only resource IDs, action code, comment, and safe business context.

### 6.2 Task, instance, and history reads lack object authorization
`getTask` and `getInstance` apply organizational-scope specifications but do not require
the current actor to be the requester, assignee, an eligible role/privilege holder, or
an authorized observer. History follows the same scope-only shape. A user sharing the
tenant/business/branch may inspect another user's workflow data if they know its ID.
Direct-ID reads must combine scope predicates with actor/object authorization.

### 6.3 Multiple assignment policies are stored but only one is used
Task creation loads all active assignment policies for a step and applies only
`policies.stream().findFirst()`. No repository ordering or documented precedence makes
that choice deterministic, and all remaining policies are ignored. Either enforce one
active assignment policy per step or define combination and precedence semantics.

### 6.4 Workflow verification is insufficient for its risk
The module has only one object-authorization test and no end-to-end runtime coverage for
spoofed actor fields, cross-user same-scope reads, transition privilege enforcement,
concurrent completion, or assignment-policy precedence. Compile-time wiring is not
sufficient evidence for a stateful authorization engine.

---

## 7. Checked, no findings

- Every `@PrivilegeApi`/`@PreAuthorize` code across the module resolves to a real
  constant registered by `BootstrapAdministrationPrivilegeProvider` — no
  endpoint references a nonexistent privilege code.
- No entity/migration table mismatches (the one apparent mismatch found by a naive
  table-name grep was explained by `V13`'s programmatic `sys_*` → `sys_priv_*` rename).
- `systemTransactionManager` usage is uniform (94/94 occurrences); the handful of
  service methods without `@Transactional` are pure delegates/computation with no
  direct repository writes.
- The `field == null || field` (default-true) null-handling convention for `active`
  flags is applied consistently across `license`, `layout`, `accesscontrol`, and
  `privilege`.
- Migration sequence `V1`–`V29` is contiguous with no gaps; spot-checked migrations'
  contents match their filenames' stated purpose.
