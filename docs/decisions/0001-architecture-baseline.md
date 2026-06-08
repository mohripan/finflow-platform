# ADR 0001: Architecture Baseline

## Status

Accepted as initial baseline.

## Context

FinFlow is intended to provide portfolio depth with a possible real product launch path, while also preparing for a role requiring Java, React Native, ReactJS, REST APIs, API Gateway, AWS, cloud-native development, and financial-system awareness. The user also requested Keycloak, CQRS, AxonIQ, Kafka-backed async communication, PostgreSQL, NoSQL, Spring Cloud technologies, Docker Compose, Minikube, Helm, MinIO, and Kafka schema contracts.

The project should be large enough to show production thinking but still practical enough to build incrementally.

## Decision

FinFlow will use:

- React Native with TypeScript for the customer mobile app.
- Expo as the React Native framework.
- Merchant flows in the MVP.
- Full KYB for merchant onboarding.
- ReactJS with TypeScript for the admin dashboard.
- Material UI for the admin dashboard.
- Spring Boot microservices for backend bounded contexts.
- Spring Cloud Gateway as the application gateway.
- Spring Cloud Config in Phase 0 for centralized service configuration.
- Eureka for service discovery.
- Keycloak for OAuth2/OIDC authentication, role management, and token issuance.
- Axon Framework and Axon Server for CQRS, command handling, domain events, and sagas/process managers.
- Kafka for integration events, notification fan-out, fraud signals, reporting projections, and analytics-style consumers.
- Avro and Schema Registry for Kafka event contracts.
- Backward compatibility mode for Avro schema evolution.
- PostgreSQL as the source-of-truth database for transactional and financial data.
- MongoDB for denormalized read models, reporting projections, and document-shaped views.
- Redis for rate limiting, caching, and short-lived coordination.
- MinIO for local S3-compatible object storage.
- Docker Compose as the first local runtime.
- Minikube as the local Kubernetes target.
- Helm charts for Kubernetes packaging.
- AWS deployment documentation as a later production-readiness track.
- IDR as the MVP currency.
- Approved KYC as a hard prerequisite before any wallet transaction.
- Automated Keycloak setup script in addition to realm configuration files.
- Generic simulated payment provider for MVP payment flows.

## Consequences

Positive:

- Strong alignment with the target job description.
- Clear financial-domain talking points.
- Keycloak avoids custom identity implementation.
- CQRS and events provide strong auditability and workflow modeling.
- Kafka provides a realistic async integration story beyond command-side CQRS.
- Avro and Schema Registry prevent services from publishing untyped or incompatible Kafka payloads.
- Backward Avro compatibility allows consumers to keep working while producers evolve schemas conservatively.
- PostgreSQL protects money-related correctness.
- MongoDB demonstrates a useful NoSQL pattern without compromising financial state.
- Minikube and Helm make Kubernetes deployment concrete without requiring cloud spend.
- MinIO keeps local KYC/KYB document storage close to the production S3 model.

Negative:

- Microservices, Keycloak, Axon, Kafka, Schema Registry, MinIO, and Kubernetes add setup complexity.
- CQRS can be overused if not constrained.
- Running the full local stack may require meaningful machine resources.
- More documentation and discipline are needed to keep the system understandable.

## Guardrails

- Do not put wallet balance source-of-truth in MongoDB or Redis.
- Do not implement custom password storage.
- Do not apply CQRS to simple CRUD screens unless there is a clear workflow or projection need.
- Do not introduce real payment-provider credentials in the repository.
- Do not allow cross-service database joins.
- Do not make asynchronous events the only protection for financial consistency; local database transactions and idempotency are still required.
- Do not allow wallet transactions before approved KYC/onboarding.
- Do not make Kafka the source of truth for financial state.
- Do not publish untyped JSON Kafka events between services.
- Do not bypass Schema Registry for integration events.
- Do not accept merchant payments before KYB approval.
- Do not bind transaction or ledger models to a specific payment provider.
