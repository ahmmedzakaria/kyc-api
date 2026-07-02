# Spring Modulith

Spring Modulith is configured to inspect and verify the backend module structure.

## Current Modules

The following top-level packages are declared as application modules:

```text
appconfigmodule
authmodule
commonmodule
gismodule
kycmodule
logmodule
posmodule
servicesmodule
```

They are currently marked as `OPEN` modules. This is intentional for incremental adoption because the existing codebase already has cross-module references into subpackages such as `dto`, `entity`, `repository`, `service`, and `config`.

## Verification

Run:

```bash
mvn test
```

The structural verification is in:

```text
src/test/java/com/nexacore/NexaCoreModulithTest.java
```

## Future Direction

As modules mature, convert selected modules from `OPEN` to stricter boundaries by exposing only stable API packages and moving implementation details behind those APIs.

Recommended order:

1. Keep `commonmodule` and `servicesmodule` stable and broadly reusable.
2. Define explicit public APIs for `posmodule`.
3. Move direct entity/repository access behind services.
4. Convert mature modules from open to closed boundaries.
5. Add explicit allowed dependencies where useful.
