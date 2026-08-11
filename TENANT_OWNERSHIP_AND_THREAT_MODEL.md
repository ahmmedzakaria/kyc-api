# Tenant ownership and threat model

## Ownership rules

| Classification | Source of truth | Examples | Required enforcement |
|---|---|---|---|
| Global identity | Auth/KYC | `auth_persons`, `kyc_person` | No tenant filter; access only through an authorized tenant relationship |
| Control plane | System | `sys_tenants`, `sys_tenant_domains` | Platform privileges; never selected from a caller tenant ID |
| Tenant account | Auth | `auth_users`, `auth_roles`, scopes and role assignments | Mandatory account tenant; assignments cannot cross it |
| Tenant relationship | KYC | person profiles, owned documents and decisions | Tenant predicate in every read and write |
| Tenant configuration/runtime | System | client assignments, workflow runtime, tenant overrides | Effective context plus repository predicate |
| Global catalog/reference | System/GIS | privilege definitions and shared reference data | Explicit global classification; tenant overrides live separately |
| Operational/audit | Log/System | access events, jobs, reconciliation | Capture effective tenant; platform reads are audited |

## Phase 3 pilot decision

`sys_client_layout_profiles` is the first tenant-owned pilot table. Layout profiles,
themes, fonts, sizes, and navigation definitions remain global catalogs; the
client-to-profile assignment is owned by exactly one tenant. Reads and direct-ID
writes require the server-derived tenant context, and the database requires a valid
tenant foreign key. Existing assignments are expanded from active, trusted client
tenant mappings. Assignments without such a mapping are quarantined and remain
inaccessible rather than receiving inferred ownership.

Every new persistent table must declare one classification in its module design and
must contain `created_by`, `updated_by`, `created_at`, and `updated_at`. Cross-database
tenant IDs are immutable application-level references validated against `sys_tenants`.

## Uniqueness and lifecycle decisions

- Tenant codes and retained hostnames are globally unique after normalization.
- Tenant-owned business keys and usernames are unique within their tenant.
- `PENDING` is onboarding-only, `ACTIVE` permits tenant traffic, `SUSPENDED` rejects
  tenant traffic while retaining data, and `CANCELLED` is terminal and retains its
  code/domain reservations.
- Global people are never duplicated merely because they operate in another tenant.

## Threats and controls

| Threat | Control |
|---|---|
| Host/header spoofing | Resolve the normalized servlet server name after trusted-proxy processing; never accept `tenantId` headers |
| Token replay through another tenant | Account tenant, resolved domain tenant, client assignment, and active scopes must intersect exactly |
| Platform-admin bypass leakage | Separate control-plane APIs and privileges; tenant repositories remain scoped |
| Native/bulk query bypass | Ownership-specific predicates and adversarial integration tests; evaluate PostgreSQL RLS before broad rollout |
| Missing async context | Messages/jobs carry a validated tenant ID and reconstruct context; missing/unknown tenants fail closed |
| Stale cache after lifecycle/domain change | Cache only successful resolution with bounded TTL and evict on every mutation |
| Cross-database partial failure | Idempotent workflows and reconciliation; never activate incomplete tenants |
