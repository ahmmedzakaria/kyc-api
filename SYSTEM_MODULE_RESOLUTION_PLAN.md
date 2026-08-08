# System Module Audit — Resolution Plan

Companion to [SYSTEM_MODULE_AUDIT_FINDINGS.md](./SYSTEM_MODULE_AUDIT_FINDINGS.md) — this
is the implementation plan for the findings that are still open (items already marked
"already fixed" in that report are not repeated here). Not yet implemented; this is the
plan to review before starting any of it.

## Guiding principles

- **Migrations stay idempotent** (`WHERE NOT EXISTS` / `ON CONFLICT DO NOTHING`),
  matching every existing migration in `db/migration/system/` — none of the fixes below
  should be a one-shot script.
- **Fix the read/write asymmetry before changing write semantics.** Where a save
  endpoint's behavior is genuinely ambiguous (insert-only entitlements, append-only
  feature privileges), add the missing read path first so the actual current behavior
  is observable and testable, then change the write behavior against that.
- **Don't widen access by default.** §1 (permission seeding) should grant the minimum
  each first-party client needs to run its own admin console, not blanket-grant
  everything — match the shape of what `system-frontend-21`/`kyc-frontend-21` actually
  call, derived from `api-registry/sync` + `api-registry/list` output for that client,
  not a hand-maintained list.
- **Verify with `mvn -q compile -DskipTests -o`** after every backend change in this
  plan, then a live pass through the relevant `system-frontend-21` admin screen where
  one exists (Layout Administration for §Layout items, License Administration for
  §License items) — the same verification loop used throughout this session.

---

## Phase 1 — Bootstrapping gaps (P0: blocks any new client from working at all)

### 1.1 Seed client API/feature permissions

**Problem**: Findings §2.1. No migration grants any client the rows in
`sys_priv_client_api_permissions`/`sys_priv_client_feature_permissions` it needs to
call its own backend.

**Approach**:
1. Run `api-registry/sync` (or read its `ApiRegistrySyncReportDto.records`) once per
   first-party client to get the authoritative list of `apiCode`s that client's own
   frontend actually calls — for `SYSTEM_ADMIN_WEB`, this is every endpoint under
   `system/access-control`, `system/privilege`, `system/layout`, `system/license` that
   `system-frontend-21` calls (see `frontendApplications/system-frontend-21/MODULES.md`
   for the current endpoint list per module).
2. Write `V30__grant_system_admin_web_permissions.sql`: idempotent inserts into both
   permission tables for `SYSTEM_ADMIN_WEB`, scoped to that endpoint list plus the
   matching `requiredPrivilegeCode`s (drawn from `BootstrapAdministrationPrivileges`,
   not invented).
3. Separately confirm whether `WEB` (kyc-frontend) has an equivalent migration anywhere
   in the history, or whether its current working state depends on manually-applied
   grants outside of migrations. If the latter, write the matching migration for `WEB`
   too — otherwise this gap will resurface the next time that database is provisioned
   fresh (a new environment, a new developer's local DB, CI).
4. Document the "how do I onboard a new client" procedure this establishes (which
   tables, which migration template) in `AGENTS.md` or this module's own docs, so the
   next new client (a future frontend app) doesn't rediscover this gap the same way.

**Blast radius**: New migration only, no code changes, no risk to existing behavior for
clients that already work.

**Verification**: Restart backend, confirm `POST /api/v1/system/privilege/context` for
`system_admin` returns non-empty `privilegeCodes`, then a full click-through of every
`system-frontend-21` admin screen (Access Control, Privileges, Layout, License) exactly
as already done live this session for License.

### 1.2 Decide the fate of `routePolicies`/`uiPolicies` for `SYSTEM_ADMIN_WEB`

**Problem**: Findings §2.2. Every `system-frontend-21` route is `public: true` at the
Angular guard level because nothing seeds these tables for it.

**Approach — this needs a decision, not just a fix**: server-side `@PrivilegeApi`
enforcement already fully protects every endpoint regardless of this gap, so this is
defense-in-depth for the frontend guard, not a security hole. Two options:
- **(a)** Seed `routePolicies`/`uiPolicies` for `system-frontend-21`'s routes the same
  way `V26` did for the Person module, and flip each route's `data: { public: true }`
  to real privilege-gated guards once seeded.
- **(b)** Leave it as-is and document explicitly (in `system-frontend-21/AGENTS.md`,
  already partially done) that this app's authorization model is intentionally
  server-only for now, with frontend route guards as a later hardening pass.

Recommend (a) only if there's an actual near-term need for role-differentiated access
*within* `system-frontend-21` (e.g. a support-tier admin who should see Client
Applications but not License Administration) — otherwise it's seeding effort with no
observable behavior change, since the backend already denies unauthorized actions.

**Verification**: If (a) is chosen — log in as a user with a subset of privileges,
confirm the sidebar and route guards hide what that user can't reach, confirm direct
URL navigation to a hidden route is blocked client-side (in addition to the existing
server-side 403).

---

## Phase 2 — Data-loss / duplication risk (P1)

### 2.1 Fix license entitlement save to be a real upsert

**Problem**: Findings §1.1. `savePlanEntitlement`/`saveEntitlementOverride` always
insert; resaving duplicates rows; there's no way to update or remove an entitlement.

**Approach**:
1. Add `id: Long | null` to `LicenseEntitlementRequestDto` (mirrors the pattern already
   used by `LicensePlanRequestDto`/`LicenseSubscriptionRequestDto`).
2. In both service methods: when `id` is present, load and update the existing row
   (same `findById(...).orElseGet(...)` pattern used by `LicensePlanServiceImpl.savePlan`);
   when absent, insert as today.
3. Now that §Phase 3 below adds `listPlanEntitlements`/`listSubscriptionEntitlements`
   responses with real `id`s, the `system-frontend-21` License → Entitlements tab can
   gain an Edit action (it's currently create-only specifically because there was no
   id to edit against) — update `LicenseEntitlementDto`/`entitlementInitialValue()`/
   the DynamicList's actions column in `license-list.component.ts` to match the
   Plans/Subscriptions tabs' Edit pattern once this lands.
4. Decide whether a full save with a changed `entitlementType` on an existing row
   should also clear the now-irrelevant target-id fields (e.g. switching from `FEATURE`
   to `MODULE` should probably null out `featureId`) — add that normalization in the
   service layer, not the frontend, so it's enforced regardless of caller.

**Blast radius**: Additive DTO field (backward compatible — existing callers that never
send `id` keep inserting exactly as today), plus new frontend Edit action. No migration
needed.

**Verification**: `mvn compile`, then live: create an entitlement, edit it (confirm no
duplicate row appears in a re-list), change its `entitlementType` and confirm stale
target-id fields clear.

### 2.2 Fix feature privilege sync to full-replace

**Problem**: Findings §1.2. `syncFeaturePrivileges` never clears old links.

**Approach**: Change it to the same delete-then-insert pattern
`LayoutUiPolicyServiceImpl` already uses correctly — delete all existing
`sys_layout_feature_privileges` rows for the feature, then insert exactly the
submitted set. This is a **behavior change users may already be relying on**
(currently, resaving with a *subset* of codes doesn't remove the others — full-replace
will start removing them), so:
1. Confirm no current caller depends on the additive behavior — check
   `system-frontend-21`'s Navigation Tree → Features tab, which is currently the only
   caller and whose `privilegeCodesText` field's docstring already explicitly warns
   about "codes to add" — this will need updating once fixed.
2. Once fixed, update the frontend field label/comment
   (`layout.model.ts:featureFields()`) from "codes to add" to "the full current set,"
   matching UI Policy's field.

**Blast radius**: Behavior change on an existing endpoint — communicate before shipping
if any other caller exists (grep the whole repo for `feature/save` callers first, not
just `system-frontend-21`).

**Verification**: Save a Feature with codes `[A, B]`, then resave with just `[A]`,
confirm `B`'s join row is actually gone (not just unduplicated).

---

## Phase 3 — Read-back gaps (P2)

### 3.1 Add getters for `ClientPermissionService` assignments

**Problem**: Findings §3.3. No way to see a client's currently-granted API permissions,
feature permissions, or tenant assignments.

**Approach**: Add `getApiPermissions(clientApplicationId)`,
`getFeaturePermissions(clientApplicationId)`, `getTenantAssignments(clientApplicationId)`
to `ClientPermissionService`, backed by the existing repositories already used to
*write* these (`ClientApiPermissionRepository`, `ClientFeaturePermissionRepository`,
and whatever backs tenant assignment — confirm exact repository name before
implementing). Add matching `client-app/api-permissions`, `client-app/feature-permissions`,
`client-app/tenant-assignments` read endpoints, gated by the same `CLIENT_APPLICATION_VIEW`
code the rest of that detail page already uses.

**Blast radius**: Purely additive (new endpoints, no changes to existing ones).
`client-application-detail.component.ts` in `system-frontend-21` should switch from
whatever local/blind state it currently tracks for these three lists to loading them
from the new endpoints on page load.

### 3.2 Add getters for role/user privilege assignment

**Problem**: Findings §3.3. `assignPrivilegesToRole`/`assignPrivilegesToUser` have no
matching "what does this role/user currently have" query.

**Approach**: Add `getRolePrivilegeCodes(roleId)`/`getUserPrivilegeCodes(userId)`
(distinct from the existing `getUserPrivilegeCodes(username)` which is scoped to the
*caller*, not an arbitrary target) to `PrivilegeService`, with matching endpoints. Lower
priority than 3.1 since no current frontend screen needs this yet — do this if/when a
role/user privilege-assignment admin screen is actually planned, not speculatively.

### 3.3 Add `metadataJson` to `LicenseSubscriptionResponseDto`

**Problem**: Findings §3.4. Write-only field.

**Approach**: One-line addition to `toResponse()` in `LicenseSubscriptionServiceImpl` —
map `subscription.getMetadataJson()` through like every other field already does.
Trivial; bundle with 2.1 since both touch the same class.

---

## Phase 4 — Design decisions needing input, not blind fixes (P3)

### 4.1 `LicensePrivilegeProvider`'s unused granular codes

**Problem**: Findings §3.2. Two live options, needs a decision:
- **(a)** Wire `LicenseController` to check the granular codes instead of the two
  umbrella ones — real fine-grained RBAC for License, matching Access Control's
  pattern, but a larger change (every endpoint's `@PrivilegeApi`/`@PreAuthorize`
  changes, plus seeding the granular codes to whichever clients need them per §1.1).
- **(b)** Remove `LicensePrivilegeProvider`'s registration — the codes are genuinely
  unused, and dead catalog entries are their own confusion risk (someone building a
  future permission-assignment UI could see them in the catalog and reasonably assume
  they're wired to something).

Recommend (b) as the default unless there's a concrete near-term plan to differentiate,
e.g., "can view License Plans but not generate Keys" as a real permission boundary
someone needs — in which case (a) is the correct investment.

### 4.2 `DataScopeService`'s tenant-scoping `@Todo`

Needs the original author's context on what "simplify" was meant to cover before
anyone else touches it — flagging for triage, not assigning a fix here.

### 4.3 DTO field-name renames (`profileCode`/`name` vs `code`/`name`, `name` vs `label`)

Not worth fixing in isolation — renaming a response field is a breaking API change for
any consumer, and the only consumer today (`system-frontend-21`) already maps around
both inconsistencies cleanly in `layout.model.ts`. Revisit only if/when a broader
Layout API v2 pass happens for unrelated reasons; until then, this finding exists so
the *next* person hitting the same surprise doesn't have to rediscover it.

---

## Explicitly not doing

- Resolving `LayoutController.java`'s `// @Todo Need to Validate with "/navigation/tree"`
  comment by deleting it — the dual-endpoint split it questions is intentional (see
  Findings §3.5) and already documented in `system-frontend-21/MODULES.md`; the TODO
  should be replaced with a comment stating that explicitly, not just deleted, so a
  future reader doesn't reopen the question from scratch. Small enough to fold into
  whichever phase above next touches `LayoutController.java`, not worth its own phase.
- Backfilling `license`/`workflow` test coverage — real gap, but a separate, larger
  effort with its own prioritization, not a byproduct of this audit's fixes.
