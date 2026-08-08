# Dynamic UI Action Privilege Implementation Plan

## Objective

Remove the remaining hard-coded numeric Person privileges from frontend components. The backend will manage privilege policies for named UI actions, while the frontend will identify only which UI action it is rendering.

This phase replaces frontend usages such as:

```html
<button *appHasPrivilege="personPrivileges.DELETE">Delete</button>
```

with:

```html
<button *appAuthorizedUi="'person.list.delete-button'">Delete</button>
```

The frontend action code identifies a control. It does not contain a privilege code, role rule, or match-mode decision.

## Terminology

Use the following names consistently:

| Layer | Name |
|---|---|
| PostgreSQL column | `action_code` |
| Java property | `actionCode` |
| JSON property | `actionCode` |
| TypeScript property | `actionCode` |
| Angular directive input | UI action code string |

`action_code` in `sys_layout_ui_policies` is scoped to UI policy identity. It is separate from `sys_priv_actions.action_code`, which identifies a privilege-catalog action such as View, Create, or Delete.

## UI Action Code Convention

Use stable, screen-qualified action codes:

```text
person.list.add-button
person.list.edit-button
person.list.delete-button
person.list.preview-button
person.list.export-button
person.preview.edit-button
```

Future workflow controls may use:

```text
person.preview.approve-button
person.preview.reject-button
person.preview.send-back-button
```

The action code must not change when the visible label or translation changes.

## Database Schema

### UI-policy table

Add a Flyway migration under the system datasource:

```sql
CREATE TABLE sys_layout_ui_policies (
    id bigserial PRIMARY KEY,
    client_application_id bigint
        REFERENCES sys_priv_client_applications(id) ON DELETE CASCADE,
    action_code varchar(150) NOT NULL,
    match_mode varchar(20) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_layout_ui_policies_match_mode
        CHECK (match_mode IN ('ANY', 'ALL'))
);
```

PostgreSQL uniqueness must handle nullable client IDs explicitly:

```sql
CREATE UNIQUE INDEX uk_sys_layout_ui_policies_global_action
    ON sys_layout_ui_policies(action_code)
    WHERE client_application_id IS NULL;

CREATE UNIQUE INDEX uk_sys_layout_ui_policies_client_action
    ON sys_layout_ui_policies(client_application_id, action_code)
    WHERE client_application_id IS NOT NULL;
```

Add an active lookup index:

```sql
CREATE INDEX idx_sys_layout_ui_policies_client_active
    ON sys_layout_ui_policies(client_application_id, active);
```

### UI-policy privilege links

```sql
CREATE TABLE sys_layout_ui_policy_privileges (
    ui_policy_id bigint NOT NULL
        REFERENCES sys_layout_ui_policies(id) ON DELETE CASCADE,
    privilege_id bigint NOT NULL
        REFERENCES sys_priv_privileges(id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ui_policy_id, privilege_id)
);

CREATE INDEX idx_sys_layout_ui_policy_privileges_policy_active
    ON sys_layout_ui_policy_privileges(ui_policy_id, active);
```

## Initial Person Policies

Create policies for the `WEB` client:

| Action code | Match mode | Required privilege |
|---|---|---|
| `person.list.add-button` | `ANY` | Person Create |
| `person.list.edit-button` | `ANY` | Person Update |
| `person.list.delete-button` | `ANY` | Person Delete |
| `person.list.preview-button` | `ANY` | Person View |
| `person.preview.edit-button` | `ANY` | Person Update |

Do not add `person.list.export-button` until a Person Export privilege and intended business rule exist. Leaving a protected control without a policy must hide it under fail-closed behavior.

The migration may create the policy rows. Because application privileges are currently synchronized after Flyway, runtime startup synchronization must create or repair privilege links after the privilege catalog exists.

## Backend Entities

Add under `com.nexacore.systemmodule.layout.entity`:

- `SysLayoutUiPolicy`
- `SysLayoutUiPolicyPrivilege`
- `SysLayoutUiPolicyPrivilegeId`

`SysLayoutUiPolicy` fields:

```java
private Long id;
private Long clientApplicationId;
private String actionCode;
private PrivilegeMatchMode matchMode;
private boolean active;
```

Use:

```java
@Table(name = "sys_layout_ui_policies")
@Column(name = "action_code", nullable = false, length = 150)
```

All persistent entities must include `created_by`, `updated_by`, `created_at`, and `updated_at`, preferably through the existing layout audit base class.

## Backend Repositories

Add:

- `LayoutUiPolicyRepository`
- `LayoutUiPolicyPrivilegeRepository`

Required queries:

1. Find a policy by client ID and `actionCode`.
2. Load active global and client-specific policy candidates.
3. Load active privilege links by policy ID.
4. Replace a policy's privilege links transactionally.

Client-specific policies must override global policies with the same `actionCode`.

## Backend DTO

Add a shared frontend-facing DTO:

```java
@Data
@Builder
public class UiPrivilegePolicyDto {
    private String actionCode;
    private String matchMode;

    @Builder.Default
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
```

The response uses `actionCode`, not `controlCode`.

## Application Context Contract

Extend `ApplicationContextDto`:

```java
@Builder.Default
private List<UiPrivilegePolicyDto> uiPolicies = new ArrayList<>();
```

Example response:

```json
{
  "privilegeCodes": [
    "01010200101",
    "01010200102"
  ],
  "uiPolicies": [
    {
      "actionCode": "person.list.add-button",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200110"]
    },
    {
      "actionCode": "person.list.edit-button",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200112"]
    },
    {
      "actionCode": "person.list.delete-button",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200140"]
    }
  ]
}
```

Keep numeric privilege codes internal to the generic authorization infrastructure. Feature components must not contain them.

## Backend UI-Policy Service

Add:

```java
public interface LayoutUiPolicyService {
    List<UiPrivilegePolicyDto> getEffectivePolicies(String clientCode);

    void synchronizePolicy(
        String clientCode,
        String actionCode,
        PrivilegeMatchMode matchMode,
        Collection<String> privilegeCodes,
        Long actorId
    );
}
```

Responsibilities:

1. Normalize and validate `actionCode`.
2. Load global and client-specific policies.
3. Prefer a client-specific policy for duplicate action codes.
4. Include only active policies, privilege links, and privileges.
5. Return policies even when the current user lacks their required privileges.
6. Replace privilege links transactionally during synchronization.
7. Reject synchronization when any requested privilege code is missing.
8. Record the authenticated or system actor in audit fields.

Do not filter policies by the current user's grants. The frontend needs denied policies to recognize that an action is protected.

## Startup Synchronization

After privilege catalog synchronization and creation of the `WEB` client, synchronize Person UI policies:

```java
uiPolicyService.synchronizePolicy(
    "WEB",
    "person.list.add-button",
    PrivilegeMatchMode.ANY,
    Set.of(personCreatePrivilegeCode),
    0L
);
```

Repeat for Edit, Delete, Preview, and preview-page Edit.

The backend may construct numeric privilege codes through its existing module/submodule/feature/action enums. The frontend must never construct them.

## Application Context Integration

In `PrivilegeServiceImpl.getApplicationContext()`:

```java
.uiPolicies(
    layoutUiPolicyService.getEffectivePolicies(clientCode)
)
```

In `AuthApplicationContextService.applyAuthPolicy()`:

```java
.uiPolicies(context.getUiPolicies())
```

Without the second change, auth-policy enrichment will discard the UI policies.

The public context must return an empty UI-policy list.

## Administration API

Add layout-management endpoints:

```text
POST /api/v1/system/layout/ui-policy/save
POST /api/v1/system/layout/ui-policy/list
POST /api/v1/system/layout/ui-policy/assign-privileges
```

Suggested save request:

```json
{
  "clientCode": "WEB",
  "actionCode": "person.list.delete-button",
  "matchMode": "ANY",
  "privilegeCodes": ["01010200140"],
  "active": true
}
```

The administration UI may later populate `actionCode`; feature frontends must not manage privilege associations directly.

## Frontend Models and Context Cache

Add:

```typescript
export interface UiPrivilegePolicy {
    actionCode: string;
    matchMode: 'ANY' | 'ALL';
    privilegeCodes: string[];
}
```

Extend the frontend application context:

```typescript
uiPolicies: UiPrivilegePolicy[];
```

`ApplicationContextService` must:

1. Default missing `uiPolicies` to `[]` during migration.
2. Store policies in a signal or equivalent reactive state.
3. Build a lookup keyed by `actionCode`.
4. clear UI policies on logout and authenticated-user changes.
5. Load a fresh context before evaluating protected UI after login.

## Shared Policy Evaluation

Extract route/UI policy evaluation into a reusable function:

```typescript
export function isPolicyAllowed(
    matchMode: 'ANY' | 'ALL',
    privilegeCodes: string[],
    hasPrivilege: (code: string) => boolean
): boolean {
    if (!privilegeCodes.length) {
        return false;
    }

    return matchMode === 'ALL'
        ? privilegeCodes.every(hasPrivilege)
        : privilegeCodes.some(hasPrivilege);
}
```

Missing policy, empty privilege codes, or an unknown match mode must fail closed.

## Dynamic Angular Directive

Add a standalone structural directive in the platform auth library:

```typescript
@Directive({
    selector: '[appAuthorizedUi]',
    standalone: true
})
export class AuthorizedUiDirective {
    private readonly template = inject(TemplateRef<unknown>);
    private readonly container = inject(ViewContainerRef);
    private readonly context = inject(ApplicationContextService);
    private readonly auth = inject(AuthService);

    readonly actionCode = input.required<string>({
        alias: 'appAuthorizedUi'
    });
}
```

The directive must:

1. Resolve the policy by `actionCode`.
2. Re-evaluate when the action code or application context changes.
3. Render only when the policy is satisfied.
4. Apply `ANY` and `ALL` consistently with route policies.
5. Render nothing when the policy is missing or empty.
6. Avoid duplicating embedded views during repeated evaluations.

Export the directive from the platform auth barrel.

## Person Template Migration

### Person list

Replace Add:

```html
<a *appAuthorizedUi="'person.list.add-button'" routerLink="/person/create">
  Add Person
</a>
```

Replace Edit:

```html
<button *appAuthorizedUi="'person.list.edit-button'" type="button">
  Edit
</button>
```

Replace Delete:

```html
<button *appAuthorizedUi="'person.list.delete-button'" type="button">
  Delete
</button>
```

Optionally protect Preview explicitly:

```html
<button *appAuthorizedUi="'person.list.preview-button'" type="button">
  Preview
</button>
```

### Person preview

Replace Edit:

```html
<a *appAuthorizedUi="'person.preview.edit-button'">
  Edit
</a>
```

Do not show approval controls until their backend workflows and policies are implemented.

## Remove Hard-Coded Frontend Privileges

After every Person UI check is migrated:

1. Remove `HasPrivilegeDirective` imports from Person components.
2. Remove `PersonPrivileges` imports.
3. Remove `personPrivileges` component fields.
4. Delete `src/app/core/auth/person-privileges.ts`.
5. Search all frontend source for `010102001` and confirm there are no matches.
6. Keep generic `AuthService.hasPrivilege()` only inside shared policy infrastructure during the transition.

## Backend API Authorization

UI policies control presentation only. They do not authorize business operations.

Backend Person endpoints must independently enforce:

| Operation | Required backend privilege |
|---|---|
| List/search | Person View/Search according to business rules |
| Preview/details/photo/documents | Person View |
| Create | Person Create |
| Update | Person Update |
| Delete | Person Delete |
| Approve | Person Approve when implemented |
| Reject | Person Reject when implemented |
| Send Back | Person Send Back when implemented |

## Tests

### Backend

- Global and client-specific action-code precedence.
- `ANY` and `ALL` serialization.
- Active/inactive policy filtering.
- Active/inactive privilege-link filtering.
- Missing privilege rejection during synchronization.
- Idempotent Person policy synchronization.
- Application-context propagation.
- Public context contains no UI policies.
- Correct Person action-code mappings.

### Frontend

- Directive renders for satisfied `ANY` policy.
- Directive renders for satisfied `ALL` policy.
- Directive hides for partially satisfied `ALL` policy.
- Missing policy fails closed.
- Empty privilege codes fail closed.
- Policy changes re-evaluate existing controls.
- Logout clears cached UI policies.
- Person buttons use action codes rather than privilege codes.
- No numeric Person privilege codes remain in frontend source.

## Implementation Sequence

1. Add UI-policy Flyway migration.
2. Add backend entities and repositories.
3. Add `UiPrivilegePolicyDto`.
4. Implement `LayoutUiPolicyService`.
5. Add UI policies to `ApplicationContextDto`.
6. Preserve policies through auth-policy enrichment.
7. Add startup synchronization for Person UI actions.
8. Add administration endpoints.
9. Add frontend policy model and context caching.
10. Extract the shared policy evaluator.
11. Implement and export `AuthorizedUiDirective`.
12. Migrate Person list and preview templates.
13. Remove `PersonPrivileges` and legacy Person directive usages.
14. Run backend, platform-library, and KYC frontend tests/builds.

## Verification Commands

```bash
cd backend
mvn test

cd ../frontendApplications/frontend-libs-21
npx ng build platform
npx ng test platform --watch=false

cd ../kyc-frontend-21
npm run build
npm test
```

## Completion Criteria

- Database and API consistently use `action_code`/`actionCode`.
- Person UI policies are backend-managed.
- Person templates contain no numeric privilege codes.
- `PersonPrivileges` is deleted.
- Missing or incomplete UI policy configuration fails closed.
- `ANY` and `ALL` work consistently for routes and UI actions.
- Backend APIs remain independently authorized.
- Backend tests, focused platform tests, and KYC production build pass.
