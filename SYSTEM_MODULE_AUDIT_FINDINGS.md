# System Module Audit — Findings

This report documents an inconsistency/gap audit of `com.nexacore.systemmodule`
(`backend/src/main/java/com/nexacore/systemmodule/`): the `accesscontrol`, `privilege`,
`layout`, `license`, and `workflow` submodules. It was produced while building the
`system-frontend-21` admin console (see `frontendApplications/system-frontend-21/MODULES.md`
for the frontend-side module reference) and covers the whole backend module, not just
what that console consumes.

Companion document: [SYSTEM_MODULE_RESOLUTION_PLAN.md](./SYSTEM_MODULE_RESOLUTION_PLAN.md)
— what to do about each finding below, in priority order.

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

### 1.2 Feature privilege sync is append-only
`LayoutNavigationServiceImpl.syncFeaturePrivileges`. Never clears or dedupes existing
`sys_layout_feature_privileges` links before inserting new ones. Resaving a Feature
with the same `privilegeCodes` — which the admin UI's edit form does by default, since
it round-trips the current codes — creates a duplicate join row every time. Contrast
with `LayoutUiPolicyServiceImpl`, which does the equivalent operation (§2.1) correctly.

---

## 2. Bootstrapping / runtime gaps

### 2.1 No client is ever seeded API/feature permissions by a migration
`V24__seed_system_admin_client_and_navigation.sql` registers the `SYSTEM_ADMIN_WEB`
client application row but never inserts into `sys_priv_client_api_permissions` or
`sys_priv_client_feature_permissions`. Checked across all 29 migrations: **no client,
including `WEB`/kyc-frontend, is ever granted these permissions by a migration** — the
only `INSERT INTO sys_priv_client_feature_permissions` statements (`V14`, `V15`) are
re-keying migrations against *existing* rows during a privilege-code realignment, not
fresh grants. `ClientAccessDecisionServiceImpl` reads directly from these two tables to
gate every non-public request (`decide()`). Whatever currently works for `WEB` was
granted by hand at some point (admin UI or direct DB write) — this is a genuine
bootstrapping gap for any new client, and it's a chicken-and-egg one: the Client
Applications admin UI that grants these permissions needs exactly these permissions to
load in the first place.

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

## 5. Workflow submodule

Not scaffolding — `workflow` (~2,100 lines across definition/execution/engine/provider
packages) is a real, wired engine: `WorkflowDefinitionController` exposes
save/list/publish/retire/provider-sync, `WorkflowRuntimeServiceImpl`/
`WorkflowTaskServiceImpl` delegate to a `WorkflowEngine` interface with a `local`
implementation, and it follows the same `systemTransactionManager`/coarse-privilege-code
conventions as `layout`/`license`. It has no frontend consumer yet
(`system-frontend-21`'s `AGENTS.md` explicitly scopes it out) and only 1 test file, so
it's verified at compile-time only, not exercised end-to-end.

---

## 6. Checked, no findings

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
