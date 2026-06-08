# FinFlow Platform

FinFlow is a portfolio-grade digital wallet and financial operations platform built to demonstrate Java, React Native, ReactJS, cloud-native architecture, API gateway patterns, AWS readiness, KYC/AML workflows, payment simulation, reporting, and technical leadership practices.

## Current Status

FinFlow is currently in the planning and architecture phase. No production code has been implemented yet.

The intended first implementation is not a proof-of-concept shortcut. It is an MVP in the product sense: a narrow, demoable vertical slice that still follows production-shaped financial-service rules such as service ownership, immutable double-entry ledger accounting, idempotent money-moving commands, audited admin decisions, Avro-backed Kafka contracts, and secure document-object handling.

## Implementation Direction

- Backend: Java, Spring Boot microservices, Spring Cloud Gateway, Spring Cloud Config, Eureka, Axon, Kafka, Avro Schema Registry, and PostgreSQL per service.
- Frontend: Expo React Native for customer and merchant flows, ReactJS with Material UI for admin operations.
- Infrastructure: Docker Compose first, Minikube and Helm after local Compose is stable.
- Storage: PostgreSQL for transactional state, MongoDB for reporting/read projections, Redis for rate limiting/cache/short-lived coordination, and MinIO for local S3-compatible document objects.
- Financial rule: Ledger is the source of truth. Wallet balances are projections and must be rebuildable from ledger events.

## Documentation Index

Start with the roadmap, architecture, and implementation start guide before creating code.

| Document | Purpose | Status |
| --- | --- | --- |
| [ARCHITECTURE.md](ARCHITECTURE.md) | High-level system architecture, service choices, storage, security, observability, and tradeoffs. | Baseline |
| [ROADMAP.md](ROADMAP.md) | Delivery phases and vertical-slice order. | Baseline |
| [docs/IMPLEMENTATION_START.md](docs/IMPLEMENTATION_START.md) | First implementation sequence and what not to build first. | New implementation guide |
| [docs/REPO_STRUCTURE.md](docs/REPO_STRUCTURE.md) | Intended monorepo layout and directory responsibilities. | New implementation guide |
| [docs/PROJECT_BRIEF.md](docs/PROJECT_BRIEF.md) | Product purpose, scope, journeys, and success criteria. | Baseline |
| [docs/BUSINESS_FLOWS.md](docs/BUSINESS_FLOWS.md) | End-to-end product flows and actor behavior. | Baseline |
| [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md) | Rules services and tests must enforce. | Baseline |
| [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) | Owned models, identifiers, state transitions, and invariants. | Baseline |
| [docs/SERVICE_BOUNDARIES.md](docs/SERVICE_BOUNDARIES.md) | Service ownership, dependencies, and workflow communication rules. | Baseline |
| [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) | REST contract conventions and first endpoint shapes. | Baseline, to be converted to OpenAPI |
| [docs/EVENT_CONTRACTS.md](docs/EVENT_CONTRACTS.md) | Kafka topic, Avro schema, event, outbox, and consumer rules. | Baseline, to be converted to Avro files |
| [docs/QUESTIONS.md](docs/QUESTIONS.md) | Answered product and architecture decisions. | No open questions |
| [docs/decisions/0001-architecture-baseline.md](docs/decisions/0001-architecture-baseline.md) | Architecture baseline ADR. | Accepted |
| [docs/decisions/0002-business-flow-baseline.md](docs/decisions/0002-business-flow-baseline.md) | Business-flow baseline ADR. | Accepted |
| [docs/decisions/0003-production-readiness-refinements.md](docs/decisions/0003-production-readiness-refinements.md) | Pre-implementation production-readiness refinements. | Accepted |

Agent-specific local context lives in [LEAN-CTX.md](LEAN-CTX.md).
