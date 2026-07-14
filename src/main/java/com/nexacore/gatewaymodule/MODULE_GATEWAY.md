# Module Gateway

Module gateway contracts provide typed, synchronous query boundaries between application modules.

Use module gateways for:

- read-only cross-module queries
- synchronous decisions that need a direct answer
- stable DTO-based summaries from an owning module

Do not use module gateways for:

- async side effects
- notifications
- audit logging
- provider routing
- generic string-based dispatch

Those cases should use Spring Modulith events, the queueing service, or ESB routes.

## Rules

- Gateway contracts live in `gatewaymodule`.
- Gateway implementations live in the owning module.
- Gateways must return DTOs, not entities.
- Gateways must not expose repositories.
- Gateways should be read-only unless a specific command boundary is deliberately introduced.
- Gateway methods should be use-case specific, not generic `call(module, action, payload)` methods.

## Example

```text
authmodule
  -> PersonModuleGateway interface
  -> kycmodule-owned implementation
  -> kyc_person data
```

Privilege decisions should use `PrivilegeModuleGateway` instead of depending on
auth privilege service internals:

```text
business module
  -> PrivilegeModuleGateway
  -> systemmodule/privilege/api/SystemPrivilegeModuleGateway
  -> privilege catalog and assignments
```

License decisions should use `LicenseModuleGateway` instead of depending on
license service internals:

```text
business module
  -> LicenseModuleGateway
  -> systemmodule/license/api/SystemLicenseModuleGateway
  -> license decision service and sys_license_* tables
```

Auth user and role lookups should use `AuthModuleGateway` instead of depending on
auth repositories or entities directly:

```text
caller module
  -> AuthModuleGateway
  -> authmodule/api/AuthModuleGatewayImpl
  -> auth user and role data
```
