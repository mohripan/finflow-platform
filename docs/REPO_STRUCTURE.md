# FinFlow Repository Structure

## Purpose

This document defines the intended monorepo layout before implementation starts. The goal is to make Phase 0 concrete enough that backend services, clients, contracts, infrastructure, and documentation land in predictable places.

## Proposed Top-Level Layout

```text
finflow-platform/
  backend/
    platform/
      config-server/
      discovery-server/
      gateway/
    services/
      user-service/
      kyc-service/
      merchant-service/
      wallet-service/
      ledger-service/
      transaction-service/
      payment-service/
      fraud-service/
      notification-service/
      reporting-service/
    shared/
      java-common/
      test-support/
  mobile/
    app/
  admin/
    dashboard/
  contracts/
    avro/
      common/
      user/
      kyc/
      merchant/
      wallet/
      ledger/
      transaction/
      payment/
      fraud/
      notification/
      audit/
    openapi/
      user-service/
      kyc-service/
      merchant-service/
      wallet-service/
      ledger-service/
      transaction-service/
      payment-service/
      fraud-service/
      notification-service/
      reporting-service/
  infrastructure/
    docker-compose/
    keycloak/
    config/
    helm/
      finflow/
    observability/
  docs/
    decisions/
  scripts/
  .github/
    workflows/
```

## Directory Responsibilities

### `backend/platform`

Holds Spring Cloud infrastructure applications:

- `config-server`: local Spring Cloud Config Server.
- `discovery-server`: Eureka Server.
- `gateway`: Spring Cloud Gateway, route-level authorization, correlation ID propagation, and rate limiting.

These applications are infrastructure boundaries. They must not own business data.

### `backend/services`

Holds Spring Boot bounded-context services. Each service owns its own database schema, migrations, API, tests, and event producers/consumers.

Expected service-local structure:

```text
service-name/
  src/
    main/
    test/
  db/
    migration/
  openapi/
  README.md
```

Service READMEs should explain local responsibilities, owned models, important commands, and verification commands.

### `backend/shared`

Holds reusable Java code that does not create cross-service domain coupling.

Allowed examples:

- API envelope helpers.
- correlation ID utilities.
- test containers support.
- common validation annotations.

Not allowed:

- shared JPA entities for service-owned tables.
- shared wallet, ledger, KYC, KYB, or transaction business logic.
- code that lets one service read another service's database.

### `mobile/app`

Holds the Expo React Native app for customer and merchant MVP flows. It should integrate with real gateway APIs and include loading, error, empty, and success states.

### `admin/dashboard`

Holds the ReactJS admin dashboard with Material UI. It should use real admin APIs through the gateway and must not fake review queues, reports, or transaction data.

### `contracts/avro`

Holds Avro schemas for Kafka integration events and shared value records. Generated classes should be built into per-domain contract modules plus a common module.

Rules:

- No untyped JSON Kafka events.
- No floating-point money fields.
- No raw identity numbers, raw bank account numbers, document bytes, signed URLs, passwords, or tokens.
- Schema compatibility checks must run in CI once schemas exist.

### `contracts/openapi`

Holds service-owned OpenAPI specs generated from or aligned with `docs/API_CONTRACTS.md`.

Rules:

- Public path parameters must use public IDs or public references.
- Every endpoint must use the documented response and error envelopes.
- Money-moving endpoints must document `Idempotency-Key`.
- OpenAPI validation should run in CI once specs exist.

### `infrastructure`

Holds local and deployment infrastructure:

- Docker Compose files.
- Keycloak realm export and setup scripts.
- Spring Cloud Config local files.
- Helm charts for Minikube.
- Observability configuration.

### `scripts`

Holds repeatable local developer scripts, such as Keycloak setup, schema validation, local smoke checks, and demo setup.

Scripts must be safe to rerun where practical.

## Phase 0 Bootstrap Order

1. Create top-level directories and empty service/client placeholders.
2. Add Docker Compose baseline for infrastructure dependencies.
3. Add Keycloak realm export and idempotent setup script.
4. Add Config Server, Eureka Server, and Gateway skeletons.
5. Add `contracts/avro` and `contracts/openapi` placeholders.
6. Add CI skeleton for formatting, tests, OpenAPI validation, and Avro compatibility checks.
7. Add Helm chart skeleton that can render with `helm template`.

## Guardrails

- Do not create fake endpoints just to satisfy client screens.
- Do not create a shared database or shared JPA model across services.
- Do not put business secrets in Config Server files.
- Do not let infrastructure bootstrap imply that money-moving flows are implemented.
- Do not add generated contract artifacts to source control unless the build strategy explicitly requires it.
