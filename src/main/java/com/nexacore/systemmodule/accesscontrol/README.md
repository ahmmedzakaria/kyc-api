# Access-Control Documentation Index

This page is the navigation entry point for backend API access control. It does
not replace the existing analyses, plans, or test roadmap.

## Documents

| Document | Type | Scope |
|---|---|---|
| [Backend API access-control analysis](BACKEND_API_ACCESS_CONTROL_ANALYSIS.md) | Analysis/reference | Current architecture and implementation assessment |
| [API access-control implementation plan](API_ACCESS_CONTROL_IMPLEMENTATION_PLAN.md) | Plan | Client and API access-control delivery |
| [Tenant/client authentication policy](TENANT_CLIENT_AUTH_POLICY_IMPLEMENTATION_PLAN.md) | Plan | Tenant- and client-scoped authentication policy |
| [Tenant authentication change-set review](TENANT_AUTH_CHANGESET_SYSTEM_IMPACT_REVIEW_AND_IMPLEMENTATION_PLAN.md) | Review/plan | System impact and implementation sequence |
| [Access-control test roadmap](TENANT_PRIVILEGE_API_ACCESS_CONTROL_TEST_ROADMAP.md) | Roadmap | Tenant, privilege, and API verification |

## Runtime flow

1. Authentication and tenant-resolution filters establish client and tenant context.
2. [`TenantAccountResolver`](../../authmodule/security/service/TenantAccountResolver.java) verifies the active client and its assignment to the resolved tenant.
3. `ClientApiAccessFilter` and `UserPrivilegeApiAccessFilter` evaluate API and user privilege requirements.
4. [`AuthorizationEventEmitter`](security/AuthorizationEventEmitter.java) publishes an immutable `AuthorizationDecisionEvent` and emits structured JSON logging.
5. `AccessControlMetrics` consumes authorization decisions for metrics.

## Related architecture

- [Authorization overview](../../../../../../../../docs/architecture/authorization.md)
- [Authentication and SSO](../../../../../../../../docs/architecture/authentication-and-sso.md)
- [Tenancy](../../../../../../../../docs/architecture/tenancy.md)
- [Observability](../../../../../../../../docs/architecture/observability.md)
- [Tenant ownership and threat model](../../../../../../../TENANT_OWNERSHIP_AND_THREAT_MODEL.md)
- [Frontend access-control architecture](../../../../../../../../frontendApplications/FRONTEND_TENANT_PRIVILEGE_API_ACCESS_CONTROL_ARCHITECTURE_PLAN.md)
