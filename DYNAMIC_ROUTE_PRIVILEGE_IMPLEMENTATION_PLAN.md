# Dynamic Route Privilege Implementation Plan

## Objective

Remove hard-coded numeric privilege codes and capability constants from the frontend. The backend will publish authorization policies associated with route URLs and UI-control identifiers. The frontend will use generic guards and directives to evaluate those policies.

The backend remains the source of truth for privilege codes, role assignments, client permissions, route authorization, and privilege match modes.

## Current Problem

The KYC frontend currently imports a hard-coded `PersonPrivileges` object and declares explicit privilege codes in route guards:

```typescript
canActivate: [privilegeGuard(PersonPrivileges.UPDATE)]
```

Replacing numeric codes with hard-coded capability names would only move the duplication. It would still require frontend releases when backend privilege definitions change.

The existing navigation tree cannot completely solve route authorization because it primarily models navigable menu entries. Routes such as `/person/:id/edit` and `/person/:id/preview` do not need to appear in navigation but still require authorization.

## Target Application Context Contract

The authenticated application context will include route policies:

```json
{
  "privilegeCodes": [
    "01010200101",
    "01010200102"
  ],
  "routePolicies": [
    {
      "routeUrl": "/person",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200101"]
    },
    {
      "routeUrl": "/person/create",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200110"]
    },
    {
      "routeUrl": "/person/:id/edit",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200112"]
    },
    {
      "routeUrl": "/person/:id/preview",
      "matchMode": "ANY",
      "privilegeCodes": ["01010200101"]
    }
  ]
}
```

`privilegeCodes` remains in the context during the transition. Frontend feature code must no longer contain or construct numeric privilege codes.

## Database Changes

### Route-policy table

Add a system-owned table for protected frontend routes:

```sql
CREATE TABLE sys_layout_route_policies (
    id bigserial PRIMARY KEY,
    client_application_id bigint,
    route_url varchar(255) NOT NULL,
    match_mode varchar(20) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT ck_sys_layout_route_policies_match_mode
        CHECK (match_mode IN ('ANY', 'ALL')),
    CONSTRAINT uk_sys_layout_route_policies_client_url
        UNIQUE (client_application_id, route_url)
);
```

`route_url` may contain Angular-style parameter segments, for example `/person/:id/edit`.

When `client_application_id` is null, the policy is global. Client-specific policies take precedence over global policies for the same `route_url`.

### Route-policy privilege links

```sql
CREATE TABLE sys_layout_route_policy_privileges (
    route_policy_id bigint NOT NULL
        REFERENCES sys_layout_route_policies(id) ON DELETE CASCADE,
    privilege_id bigint NOT NULL
        REFERENCES sys_priv_privileges(id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (route_policy_id, privilege_id)
);
```

Create indexes for active policy lookup by client and `route_url` and for privilege-link lookup by policy.

### Initial Person policies

Seed idempotent policies for:

| Route URL | Match mode | Required privilege |
|---|---|---|
| `/person` | `ANY` | Person View |
| `/person/create` | `ANY` | Person Create |
| `/person/:id/edit` | `ANY` | Person Update |
| `/person/:id/preview` | `ANY` | Person View |

Resolve privilege IDs from `sys_priv_privileges.privilege_code` inside the migration. Do not store privilege codes in route-policy rows.

## Backend Domain Model

Add entities under the system layout module:

- `SysLayoutRoutePolicy`
- `SysLayoutRoutePolicyPrivilege`
- `SysLayoutRoutePolicyPrivilegeId` if the link uses a composite identifier

`SysLayoutRoutePolicy` fields:

- `id`
- optional client application reference
- `routeUrl`
- `PrivilegeMatchMode matchMode`
- `active`
- standard audit fields
- active privilege links

Use explicit `@Table` mappings and the existing layout audit base class where appropriate.

## Backend DTO Contract

Add a frontend-facing DTO:

```java
@Data
@Builder
public class RoutePrivilegePolicyDto {
    private String routeUrl;
    private PrivilegeMatchMode matchMode;

    @Builder.Default
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
```

Extend `ApplicationContextDto`:

```java
@Builder.Default
private List<RoutePrivilegePolicyDto> routePolicies = new ArrayList<>();
```

Keep JSON naming camel-case (`routeUrl`). The relational column remains snake-case (`route_url`).

## Backend Service Changes

Add `LayoutRoutePolicyService` with responsibilities to:

1. Load active global and client-specific policies.
2. Apply client-specific precedence for duplicate route URLs.
3. Load only active privilege links and active privileges.
4. Return every applicable route policy, including policies the current user does not satisfy.
5. Avoid leaking unrelated client-specific policies.
6. Validate that each protected policy has at least one active privilege link.

The application-context service must not remove policies merely because the current user lacks their privileges. The frontend needs the policy to recognize the route as protected and deny access.

In `PrivilegeServiceImpl.getApplicationContext()`:

```java
Set<String> userPrivilegeCodes = getUserPrivilegeCodes(username);

ApplicationContextDto context = ApplicationContextDto.builder()
        .privilegeCodes(userPrivilegeCodes)
        .routePolicies(layoutRoutePolicyService.getEffectivePolicies(clientCode))
        // existing context fields
        .build();
```

`AuthApplicationContextService.applyAuthPolicy()` rebuilds the context and must copy `routePolicies`; otherwise the newly loaded policies will be discarded.

The public application context must return an empty route-policy list.

## Policy Matching Rules

Route matching will be implemented in one generic frontend guard.

Required behavior:

1. Remove query strings and fragments from the requested URL.
2. Normalize leading and trailing slashes.
3. Match static segments exactly.
4. Treat segments beginning with `:` in `routeUrl` as one dynamic path segment.
5. Do not allow a dynamic segment to span `/`.
6. Prefer the most specific matching policy.
7. Prefer client-specific policy over a global policy for the same URL.
8. Reject ambiguous policies during backend validation or context construction.

Specificity should be determined by:

1. More static segments.
2. More total segments.
3. Client-specific policy over global policy.

Examples:

| Route URL | Requested URL | Match |
|---|---|---|
| `/person` | `/person` | Yes |
| `/person/:id/edit` | `/person/25/edit` | Yes |
| `/person/:id/edit` | `/person/create` | No |
| `/person/:id/preview` | `/person/25/preview?tab=documents` | Yes after normalization |

## Frontend Application-Context Changes

Add the model:

```typescript
export interface RoutePrivilegePolicy {
    routeUrl: string;
    matchMode: 'ANY' | 'ALL';
    privilegeCodes: string[];
}
```

Extend the frontend `ApplicationContext` interface with:

```typescript
routePolicies: RoutePrivilegePolicy[];
```

Ensure `ApplicationContextService`:

- unwraps `routePolicies` safely;
- defaults missing policies to `[]` during the transition;
- stores them in the in-memory application context;
- clears them on logout and user changes;
- does not treat another user's cached context as current.

## Generic Route Guard

Replace `privilegeGuard(...)` and the route-specific numeric constants with one `routePrivilegeGuard` registered as `canActivateChild`.

Guard evaluation:

1. Explicit `data.public === true` returns `true`.
2. Ensure the authenticated application context is loaded.
3. Find the most specific policy matching the requested URL.
4. If no policy matches a non-public application route, fail closed.
5. If a matched policy has no privilege codes, fail closed.
6. For `ANY`, allow when the user holds at least one required code.
7. For `ALL`, allow only when the user holds every required code.
8. Redirect denied navigation to `/dashboard` or a dedicated forbidden page.

The guard remains generic. It reads numeric codes from the backend response but contains no application privilege codes of its own.

## Target Frontend Routes

After migration, `app.routes.ts` will contain no `PersonPrivileges` import and no route-specific `privilegeGuard` calls:

```typescript
import {
    AUTH_ROUTES,
    authGuard,
    routePrivilegeGuard
} from '@nexacore/platform';

export const routes: Routes = [
    ...AUTH_ROUTES,
    {
        path: '',
        loadComponent: () =>
            import('@nexacore/platform').then(m => m.LayoutComponent),
        canActivate: [authGuard],
        canActivateChild: [routePrivilegeGuard],
        children: [
            {
                path: 'dashboard',
                loadComponent: () =>
                    import('./pages/dashboard/dashboard.component')
                        .then(m => m.DashboardComponent),
                data: { public: true }
            },
            {
                path: 'person',
                loadComponent: () =>
                    import('./pages/person/person-list/person-list.component')
                        .then(m => m.PersonListComponent)
            },
            {
                path: 'person/create',
                loadComponent: () =>
                    import('./pages/person/person-editor.component')
                        .then(m => m.PersonEditorComponent)
            },
            {
                path: 'person/:id/edit',
                loadComponent: () =>
                    import('./pages/person/person-editor.component')
                        .then(m => m.PersonEditorComponent)
            },
            {
                path: 'person/:id/preview',
                loadComponent: () =>
                    import('./pages/person/person-preview.component')
                        .then(m => m.PersonPreviewComponent)
            }
        ]
    }
];
```

## Navigation Integration

Navigation visibility and route authorization serve related but different purposes:

- `sys_layout_features` controls which entries appear in navigation.
- `sys_layout_route_policies` controls whether a URL may activate.

For navigable routes such as `/person`, both configurations must refer to the same privilege. Add a backend validation test or synchronization service to detect disagreement between a layout feature's active privileges and the corresponding route policy.

Do not create visible navigation features merely to protect non-navigable edit or preview routes.

## Dynamic UI-Control Authorization

Route policies do not govern buttons displayed within an already-authorized page. Add backend-managed UI policies in a later phase:

```json
{
  "uiPolicies": {
    "person.add-button": {
      "matchMode": "ANY",
      "privilegeCodes": ["01010200110"]
    },
    "person.edit-button": {
      "matchMode": "ANY",
      "privilegeCodes": ["01010200112"]
    },
    "person.delete-button": {
      "matchMode": "ANY",
      "privilegeCodes": ["01010200140"]
    }
  }
}
```

The frontend directive will receive a UI-control identifier, not a privilege code:

```html
<button *appAuthorizedUi="'person.delete-button'">Delete</button>
```

The backend owns the identifier-to-privilege association.

## Backend API Enforcement

Frontend route guards and hidden buttons are not security boundaries. Person endpoints must continue to enforce effective user privileges on the backend.

Required mappings include:

| API operation | Required privilege |
|---|---|
| Search/list persons | Person View and/or Search according to the final business rule |
| Read person details/photo/documents | Person View |
| Create person | Person Create |
| Update person | Person Update |
| Delete person | Person Delete |
| Approve person | Person Approve when implemented |
| Reject person | Person Reject when implemented |
| Send person back | Person Send Back when implemented |

Route policies must never replace API authorization.

## Administration API

Add system-layout endpoints for managing route policies:

- save a route policy;
- list route policies by client;
- activate/deactivate a policy;
- assign privilege links;
- validate conflicting or ambiguous route URLs.

Administrative writes must record the authenticated actor in `created_by` and `updated_by`.

## Migration Sequence

1. Add route-policy tables and indexes.
2. Seed Person route policies and privilege links.
3. Add backend entities, repositories, DTOs, and services.
4. Add route policies to `ApplicationContextDto`.
5. Preserve policies through `AuthApplicationContextService`.
6. Add frontend application-context models.
7. Implement and test parameterized route matching.
8. Add the generic `routePrivilegeGuard`.
9. Switch `app.routes.ts` to the generic guard.
10. Verify all protected routes before removing explicit guards.
11. Add backend-managed UI-control policies and migrate buttons.
12. Delete `person-privileges.ts` after no consumers remain.
13. Remove the legacy frontend `privilegeGuard` only after all frontend applications migrate.

## Verification Plan

### Backend tests

- Global and client-specific policy selection.
- Client-specific precedence over global policies.
- Active/inactive policy filtering.
- Active/inactive privilege-link filtering.
- `ANY` and `ALL` serialization.
- Empty-link policy detection.
- Person policy seed idempotency.
- Application-context propagation through auth-policy enrichment.
- Public context contains no protected policies.
- Layout navigation and route policy consistency for `/person`.

### Frontend tests

- Exact route matching.
- Parameterized route matching.
- Query-string and fragment normalization.
- Static route precedence over parameterized routes.
- `ANY` permits one matching privilege.
- `ALL` requires every privilege.
- Matched policy with empty codes fails closed.
- Unmatched non-public route fails closed.
- Explicit public route succeeds.
- Cached context is cleared when the authenticated user changes.
- No numeric Person privilege codes remain in frontend source.

### Build checks

```bash
cd backend
mvn test

cd ../frontendApplications/kyc-frontend-21
npm run build
```

## Completion Criteria

- `app.routes.ts` contains no numeric privilege codes or Person privilege constants.
- `person-privileges.ts` is removed.
- Route authorization comes entirely from backend `routePolicies`.
- The database and API consistently use `route_url`/`routeUrl` naming.
- Navigable and non-navigable Person routes are protected.
- Route guards fail closed for missing or incomplete protected-route configuration.
- Backend Person APIs independently enforce privileges.
- Backend and frontend automated tests pass.
