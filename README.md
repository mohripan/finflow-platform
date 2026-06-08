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

Start here:

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [ROADMAP.md](ROADMAP.md)
- [docs/BUSINESS_FLOWS.md](docs/BUSINESS_FLOWS.md)
- [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md)
- [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)
- [docs/SERVICE_BOUNDARIES.md](docs/SERVICE_BOUNDARIES.md)
- [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md)
- [docs/EVENT_CONTRACTS.md](docs/EVENT_CONTRACTS.md)
- [docs/PROJECT_BRIEF.md](docs/PROJECT_BRIEF.md)
- [docs/QUESTIONS.md](docs/QUESTIONS.md)
- [docs/decisions/0001-architecture-baseline.md](docs/decisions/0001-architecture-baseline.md)
- [docs/decisions/0002-business-flow-baseline.md](docs/decisions/0002-business-flow-baseline.md)
- [docs/decisions/0003-production-readiness-refinements.md](docs/decisions/0003-production-readiness-refinements.md)
