# FinFlow Implementation Start Guide

## Purpose

This guide defines the first implementation sequence for FinFlow. It is intentionally narrower than the full roadmap so the first code changes establish repeatable structure without pretending that business flows already work.

## Starting Assumptions

- The repository begins as documentation-only.
- Phase 0 comes before business services.
- Local development uses Docker Compose first.
- Minikube and Helm are prepared after Compose is stable.
- Backend services are Spring Boot applications.
- Mobile is Expo React Native.
- Admin is ReactJS with Material UI.

## First PR Sequence

### PR 1: Monorepo Skeleton

Create the directories described in `docs/REPO_STRUCTURE.md`.

Minimum result:

- backend, mobile, admin, contracts, infrastructure, scripts, and CI folders exist.
- placeholder READMEs explain ownership and planned verification commands.
- no fake APIs or fake UI data are introduced.

Verification:

```text
Repository tree matches docs/REPO_STRUCTURE.md.
Documentation links remain valid.
```

### PR 2: Local Infrastructure Baseline

Add Docker Compose for:

- Keycloak.
- PostgreSQL.
- MongoDB.
- Redis.
- MinIO.
- Axon Server.
- Kafka.
- Schema Registry.

Minimum result:

- infrastructure starts locally.
- health endpoints or readiness checks are documented.
- no business service depends on hardcoded production secrets.

Verification:

```text
docker compose up
docker compose ps
```

### PR 3: Identity And Platform Services

Add:

- Keycloak realm export and idempotent setup script.
- Spring Cloud Config Server.
- Eureka Server.
- Spring Cloud Gateway skeleton.

Minimum result:

- Keycloak can issue local tokens.
- Gateway exposes health.
- Config Server serves local config.
- Eureka shows registered platform services.

Verification:

```text
Keycloak token request succeeds.
Gateway health endpoint responds.
Config Server returns local configuration.
Eureka dashboard shows registered services.
```

### PR 4: Contract Tooling

Add contract folders and validation tooling for:

- Avro schemas.
- OpenAPI specs.

Minimum result:

- common Avro value schemas can compile.
- initial OpenAPI skeletons can be linted.
- CI has placeholder jobs that can be filled as services appear.

Verification:

```text
Avro schema compile task passes.
OpenAPI lint task passes.
```

### PR 5: User Service Vertical Foundation

Add the first real service slice:

- User Service profile creation/loading from Keycloak subject.
- gateway route.
- API envelope.
- persistence migration.
- tests for profile creation idempotency.

Minimum result:

- authenticated `GET /api/v1/users/me` creates or loads an application profile.
- unauthenticated requests fail.
- response follows `docs/API_CONTRACTS.md`.

Verification:

```text
User Service tests pass.
Gateway route smoke test passes.
Profile creation is idempotent for the same Keycloak subject.
```

## What Not To Build First

- Money-moving transaction endpoints.
- Wallet balance mutation.
- merchant payment screens.
- fake admin queues.
- fake reporting dashboards.
- Kafka events without Avro schemas.
- frontend screens backed by hardcoded production-path data.

## First Business Slice After Foundation

After Phase 0 and identity/profile foundation, start with customer KYC and admin review before wallet or payment flows. This preserves the hard rule that wallet transactions require approved KYC.

Minimum first business flow:

1. Customer signs in.
2. User profile is created.
3. Customer submits KYC metadata and documents through real object-upload flow.
4. Admin reviews KYC.
5. KYC decision creates audit record.
6. KYC approval can trigger wallet activation in the later wallet slice.

## Documentation Updates During Implementation

Update the relevant docs in the same change when implementation changes:

- API shape or status codes.
- event names, fields, or schemas.
- service ownership.
- business rules.
- roadmap phase scope.
- infrastructure commands.
- major architecture decisions.
