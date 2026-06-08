# FinFlow Agent Instructions

## Project Brief

FinFlow is a portfolio-grade digital wallet and merchant payment platform with a possible real-product launch path. It uses Java/Spring Boot microservices, Expo React Native, ReactJS admin dashboard, Keycloak, Spring Cloud Gateway, Spring Cloud Config, Eureka, Axon, Kafka, Avro Schema Registry, PostgreSQL, MongoDB, Redis, MinIO, Docker Compose, Minikube, and Helm.

The business model includes customer KYC, merchant KYB, wallet activation, available and pending balances, top-up, QR-based customer transfer, fixed-amount merchant QR payment, configurable flat merchant fees, merchant withdrawal with simulated payout callback, admin-approved refunds, blocking fraud review with admin retry, reporting, notifications, audit logs, and immutable double-entry ledger accounting.

## Required Reading

Before making implementation decisions, read the relevant docs:

- [README.md](README.md)
- [docs/PROJECT_BRIEF.md](docs/PROJECT_BRIEF.md)
- [docs/BUSINESS_FLOWS.md](docs/BUSINESS_FLOWS.md)
- [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md)
- [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)
- [docs/SERVICE_BOUNDARIES.md](docs/SERVICE_BOUNDARIES.md)
- [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md)
- [docs/EVENT_CONTRACTS.md](docs/EVENT_CONTRACTS.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [ROADMAP.md](ROADMAP.md)
- [docs/decisions/0001-architecture-baseline.md](docs/decisions/0001-architecture-baseline.md)
- [docs/decisions/0002-business-flow-baseline.md](docs/decisions/0002-business-flow-baseline.md)
- [docs/decisions/0003-production-readiness-refinements.md](docs/decisions/0003-production-readiness-refinements.md)

## Hard Rules

- Never fake an endpoint, workflow, event, or data path just to make a screen look complete.
- Never return hardcoded business data from production code paths.
- Never bypass service boundaries or read another service's database directly.
- Never treat wallet balance as the financial source of truth; ledger is the source of truth.
- Never mutate posted ledger entries. Use reversal journals.
- Never use floating-point numbers for money.
- Never allow wallet transactions before customer KYC approval.
- Never allow merchant payment or withdrawal before merchant KYB approval.
- Never publish untyped JSON Kafka integration events. Use Avro and Schema Registry.
- Never include raw identity numbers, raw bank account numbers, document bytes, passwords, tokens, or signed object URLs in Kafka events.
- Never implement custom password storage; Keycloak owns authentication.
- Never skip idempotency for money-moving commands.
- Never silently ignore failed tests, failing containers, broken migrations, schema incompatibility, or end-to-end flow failures.

## Implementation Expectations

- Build vertical slices that are demoable end to end.
- Prefer working, tested flows over broad scaffolding that does nothing.
- Every API route must be backed by real service logic, validation, persistence, and tests.
- Every event producer must use a defined Avro schema and be covered by producer tests.
- Every event consumer must be idempotent and covered by duplicate-event tests.
- Every money movement must have ledger entries and tests proving the journal balances.
- Every admin decision must create an audit record.
- Every state transition must be explicit and testable.
- Every service owns its own schema and migrations.
- Every public API must follow the response and error envelope in [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md).
- Every money-moving endpoint must require `Idempotency-Key`.
- All timestamps must be UTC.

## Testing Requirements

For any implemented feature, add or update tests at the right level:

- Unit tests for domain rules and state transitions.
- Integration tests for persistence, migrations, repositories, and service adapters.
- Contract tests for REST/OpenAPI behavior.
- Contract tests for Avro schema compatibility and Kafka serialization.
- Consumer tests for duplicate event handling.
- End-to-end tests for complete business flows once the required services exist.

Minimum end-to-end flows to protect as the platform grows:

- Customer signs in, submits KYC, admin approves, wallet activates.
- Merchant owner submits KYB, admin approves, merchant wallet activates.
- Customer tops up through simulated payment.
- Customer transfers by QR.
- Customer pays merchant QR with flat merchant fee ledger accounting.
- Merchant withdrawal goes pending and completes through simulated payout callback.
- Merchant requests refund, admin approves, ledger posts reversal.
- Fraud review blocks transaction, admin resolves, transaction is retried.

## Architecture Guardrails

- Use Spring Boot services around the documented bounded contexts.
- Use Spring Cloud Gateway for public routing.
- Use Spring Cloud Config and Eureka as part of the platform baseline.
- Use Axon for command/event workflows and sagas where workflow state matters.
- Use Kafka for integration events, reporting, notifications, audit streams, and analytics.
- Use PostgreSQL for transactional source-of-truth data.
- Use MongoDB for reporting/read projections only.
- Use Redis for rate limiting, caching, and short-lived coordination only.
- Use MinIO locally for KYC/KYB documents.
- Use Docker Compose for local infrastructure and Helm for Minikube.

## Frontend Guardrails

- Use Expo React Native for the mobile app.
- Use ReactJS with Material UI for the admin dashboard.
- Do not build static mock screens that are disconnected from backend contracts.
- Prefer real API integration with loading, error, empty, and success states.
- Do not hide incomplete backend behavior behind fake client data.

## Documentation Rules

- Update the relevant docs when behavior, APIs, events, service ownership, or business rules change.
- Record major architecture or business decisions as ADRs under `docs/decisions/`.
- If a decision conflicts with existing docs, update the docs in the same change.
- Keep examples consistent with IDR, minor-unit money, KYC-before-transaction, KYB-before-merchant-payment, and ledger immutability.

## Definition Of Done

A task is not done until:

- The implementation matches the documented business rule and service boundary.
- Tests cover the success path and important edge cases.
- Migrations/config/schema files are included where needed.
- Local commands needed to verify the change are documented or run.
- No endpoint, UI state, event, or report relies on fake production-path data.
- End-to-end behavior is verified when the task affects a user-visible workflow.
