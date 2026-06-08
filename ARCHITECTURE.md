# FinFlow Architecture

## Architecture Goals

FinFlow is designed to show production-style full-stack and technical leadership capability:

- Mobile-first customer and merchant experience with Expo React Native.
- Web-based operations dashboard with ReactJS.
- Java/Spring Boot backend organized by financial bounded contexts.
- Keycloak-based authentication and authorization.
- CQRS and event-driven workflows using Axon Framework, Axon Server, and Kafka.
- Strong financial correctness through immutable ledger records, idempotency, and audit logs.
- Cloud-native deployment path using Docker, Spring Cloud, and AWS-friendly infrastructure.

## High-Level System

```text
React Native Customer App
React Native Merchant MVP Flows
ReactJS Admin Dashboard
        |
        v
Spring Cloud Gateway
        |
        v
Keycloak token validation and route-level authorization
        |
        v
-------------------------------------------------------------
| Auth Integration | User Service | KYC Service | Merchant Service |
| Wallet Service   | Ledger Service | Transaction Service     |
| Payment Service  | Fraud Service | Notification Service    |
| Reporting Service                                      |
-------------------------------------------------------------
        |
        v
PostgreSQL per service, MongoDB read models/documents, Redis cache
        |
        v
Axon Server command/event infrastructure, Kafka integration events, and Schema Registry
```

## Client Applications

### Expo React Native Customer App

Responsibilities:

- Login and registration redirect through Keycloak.
- Wallet dashboard.
- Top-up flow.
- Peer-to-peer transfer.
- QR payment generation and scanning.
- Merchant payment flow.
- Transaction history.
- KYC submission.
- Profile and security settings.
- In-app notification center.

Recommended libraries:

- React Native with TypeScript.
- Expo.
- React Navigation.
- TanStack Query.
- Redux Toolkit for app and workflow state.
- React Hook Form and Zod.
- Secure token storage using platform keychain/keystore.

### Merchant MVP Flows

Merchant support is included in the MVP because it strengthens the financial-platform story and makes QR payments more realistic.

Responsibilities:

- Merchant registration and profile setup.
- Full KYB submission.
- Business registration details.
- Business owner identity linkage.
- Business document metadata.
- Admin approval/rejection workflow.
- Merchant approval status.
- Merchant QR payment request generation.
- Incoming payment history.
- Basic merchant transaction reporting.

### ReactJS Admin Dashboard

Responsibilities:

- KYC review queue.
- User and wallet status management.
- Transaction monitoring.
- Fraud alert review.
- Reports and charts.
- Audit log inspection.

Recommended libraries:

- React with TypeScript and Vite.
- TanStack Query.
- React Hook Form and Zod.
- Material UI.
- Recharts for reporting.

## Backend Services

### Spring Cloud Gateway

Responsibilities:

- Single public backend entry point.
- JWT validation against Keycloak.
- Route-based authorization.
- Rate limiting with Redis.
- Request correlation ID propagation.
- API version routing.

### Identity Boundary

Keycloak is the identity provider. FinFlow should not build password management itself.

Responsibilities:

- User registration and login.
- OAuth2/OIDC token issuance.
- Realm roles and client roles.
- Admin, customer, merchant, support, compliance, and service-account roles.
- Optional OTP/MFA policies.
- Automated realm/client/role setup script for local development.

Application services store domain profile data, not passwords.

### User Service

Responsibilities:

- Customer profile.
- Account status.
- Link application user IDs to Keycloak subject IDs.
- Preferences.
- Account lifecycle events.

Primary database: PostgreSQL.

### KYC Service

Responsibilities:

- KYC application submission.
- Document metadata.
- KYC review status.
- Admin approval/rejection workflow.
- KYC audit trail.

Primary database: PostgreSQL.

Document storage:

- MinIO for local S3-compatible development.
- AWS S3 for production architecture.

MongoDB can be used for denormalized KYC review read models if the dashboard benefits from flexible filtering.

KYC rule:

- Customers and merchants must be approved before any wallet transaction is allowed.

### Merchant Service

Responsibilities:

- Merchant profile.
- Merchant onboarding status.
- Merchant payment acceptance configuration.
- Merchant withdrawal destination configuration.
- Merchant settlement-style reporting inputs.
- Link merchant owner IDs to Keycloak subject IDs.

Primary database: PostgreSQL.

### Wallet Service

Responsibilities:

- Wallet creation.
- Current available and pending wallet balance projections.
- Wallet status: active, frozen, closed.
- Available balance display and non-authoritative pre-checks.
- Projection updates from posted ledger journals and payout/refund workflow events.

Primary database: PostgreSQL.

Important rule:

- Wallet balance is not the source of truth. The immutable ledger is the financial source of truth.
- Instant money movements are protected through Transaction Service idempotency, Ledger Service duplicate-journal protection, database constraints, and serialized Ledger Service validation of authoritative balances. Wallet Service must not create independent balance-only reservations for completed transfer or merchant payment flows.
- Wallet Service may provide projection pre-checks for user experience, but Ledger Service is the only service that can reject or accept a debit for insufficient authoritative posted balance.
- Pending money movements, such as merchant withdrawal, are represented by posted ledger movement into clearing accounts and exposed through pending balance projections.

### Ledger Service

Responsibilities:

- Immutable double-entry journal.
- Ledger accounts.
- Debit and credit entries.
- Reconciliation support.
- Accounting reports.
- Duplicate journal protection by transaction step reference.
- Reversal journals for failed payout, refund, and correction workflows.
- Authoritative account-balance validation during journal posting.
- Idempotent bootstrap of system accounts such as payment clearing, payout clearing, fee revenue, and suspense.

Primary database: PostgreSQL.

Rules:

- Every financial transaction must balance.
- Ledger entries are append-only.
- Corrections are represented by reversal entries, not updates.
- Debit and credit entry amounts are always positive. Direction carries accounting meaning.
- Journal posting serializes affected ledger accounts, calculates balances from posted entries inside the same transaction, and rejects insufficient funds for wallet and merchant-balance accounts.

Example transfer:

```text
Debit:  Customer A wallet account  100000 IDR
Credit: Customer B wallet account  100000 IDR
```

Example top-up:

```text
Debit:  External payment clearing account  100000 IDR
Credit: Customer wallet account            100000 IDR
```

Example merchant payment with fee:

```text
Debit:  Customer wallet account           50000 IDR
Credit: Merchant business balance account 49000 IDR
Credit: Fee revenue account                1000 IDR
```

Example full merchant payment refund:

```text
Debit:  Merchant business balance account 49000 IDR
Debit:  Fee revenue account                1000 IDR
Credit: Customer wallet account           50000 IDR
```

### Transaction Service

Responsibilities:

- Transfer workflow.
- Top-up workflow.
- QR payment workflow.
- Merchant payment workflow.
- Merchant withdrawal workflow.
- Refund workflow.
- Configurable flat merchant payment fee workflow.
- Admin-approved refund workflow.
- Admin fraud-review retry workflow.
- Transaction status lifecycle.
- Idempotency keys for write requests.
- Coordination with Wallet, Ledger, Payment, Fraud, and Notification contexts.

Axon usage:

- Commands represent user intent.
- Events represent accepted facts.
- Sagas/process managers coordinate multi-step flows.
- Query models support transaction history and admin monitoring.

Kafka usage:

- Publish cross-service integration events for read models, notifications, reporting, analytics, and external-style async subscribers.
- Keep financial command decisions inside transactional service boundaries and Axon workflows.
- Treat Kafka events as integration facts, not as the only consistency mechanism for money movement.

### Payment Service

Responsibilities:

- Simulated payment gateway.
- Payment authorization and callback simulation.
- Payout/withdrawal simulation.
- Simulated payout callback handling.
- Payment status mapping.
- Payment failure scenarios.

This service uses a generic simulated provider in the MVP. The integration boundary should stay provider-neutral so a real payment provider can be added later without changing transaction and ledger models.

### Fraud Service

Responsibilities:

- Suspicious transaction rules.
- Velocity checks.
- Large transfer alerts.
- New device or abnormal activity signals.
- Account freeze recommendations.

First rules:

- Flag transfers above a configured threshold.
- Flag too many transfers within a short time window.
- Flag repeated failed payment attempts.

### Notification Service

Responsibilities:

- In-app notification events.
- Email or push notification abstraction.
- Notification templates.
- Delivery status tracking.

Initial implementation can store in-app notifications and log external delivery.

### Reporting Service

Responsibilities:

- Admin reporting read models.
- Daily transaction volume.
- Top-up and transfer totals.
- Failed transaction counts.
- KYC approval metrics.
- Fraud alert metrics.

Primary database: MongoDB for flexible read models, or PostgreSQL if relational reporting is enough. Initial recommendation is MongoDB because it demonstrates a clear NoSQL use case for denormalized query projections.

## CQRS And Axon Model

FinFlow should use CQRS where it adds clarity:

- Write side validates commands and emits events.
- Read side subscribes to events and builds query-optimized projections.
- Commands should not return rich read data.
- Query APIs should read projections, not command aggregates.

Candidate aggregates:

- `WalletAggregate`
- `TransactionAggregate`
- `KycApplicationAggregate`
- `LedgerJournalAggregate`
- `MerchantAggregate`

Candidate sagas/process managers:

- `TopUpSaga`
- `TransferSaga`
- `KycApprovalSaga`
- `MerchantOnboardingSaga`
- `FraudReviewSaga`

Candidate domain events:

These names are conceptual workflow events for Axon modeling. Kafka integration event names and payloads are governed by [docs/EVENT_CONTRACTS.md](docs/EVENT_CONTRACTS.md). When implementation starts, prefer the contract names where the same business fact is published externally.

- `UserProfileCreated`
- `KycSubmitted`
- `KycApproved`
- `KycRejected`
- `MerchantRegistered`
- `KybApproved`
- `MerchantActivated`
- `MerchantPaymentRequested`
- `MerchantPaymentCompleted`
- `MerchantWithdrawalRequested`
- `MerchantWithdrawalPending`
- `MerchantWithdrawalCompleted`
- `RefundRequested`
- `RefundApproved`
- `RefundRejected`
- `RefundCompleted`
- `MerchantFeeCharged`
- `FraudReviewRetryRequested`
- `WalletCreated`
- `WalletFrozen`
- `TopUpRequested`
- `PaymentPaid`
- `PaymentFailed`
- `TransferRequested`
- `LedgerJournalPosted`
- `TransferCompleted`
- `FraudReviewRequired`
- `NotificationRequested`

## Kafka Integration Model

Axon is the primary command, domain event, and saga backbone for CQRS workflows. Kafka is used for broader asynchronous integration.

Use Kafka for:

- Reporting projections.
- Notification fan-out.
- Fraud signal ingestion.
- Audit stream consumers.
- Analytics-style consumers.
- Future external integration adapters.

Initial topics:

- `finflow.user.events`
- `finflow.kyc.events`
- `finflow.merchant.events`
- `finflow.wallet.events`
- `finflow.transaction.events`
- `finflow.ledger.events`
- `finflow.payment.events`
- `finflow.fraud.events`
- `finflow.notification.events`
- `finflow.audit.events`

Kafka message rules:

- Messages use Avro schemas registered in Schema Registry.
- Schema Registry compatibility mode is backward.
- Messages include event ID, event type, aggregate or entity ID, correlation ID, causation ID, occurred-at timestamp, schema version, and payload fields defined by the schema.
- Producers must serialize through generated or schema-bound Avro classes.
- Consumers must deserialize through Schema Registry and reject incompatible messages.
- Consumers must be idempotent.
- Publishing should use an outbox pattern when events originate from a local database transaction.
- CI must validate schema compatibility before service changes are merged.

## Data Storage

### PostgreSQL

Use PostgreSQL for transactional consistency:

- User profile data.
- KYC workflow state.
- Wallet state.
- Transaction state.
- Ledger journal and entries.
- Payment records.
- Idempotency records.
- Audit logs.

Each service owns its schema or database. Cross-service joins are not allowed.

### MongoDB

Use MongoDB for read-heavy or document-shaped models:

- Admin dashboard projections.
- Reporting snapshots.
- Notification inbox documents.
- Optional KYC review projections.

MongoDB should not be the source of truth for money movement.

### Redis

Use Redis for:

- Gateway rate limiting.
- Short-lived idempotency or request locks when useful.
- Cache for safe read data.

Redis must not be the source of truth for financial state.

## API Style

External APIs are RESTful JSON APIs behind Spring Cloud Gateway.

Internal coordination uses Axon commands/events/queries for financial command workflows, Kafka for broad integration events, and direct REST only for gateway-facing APIs, admin APIs, non-financial lookups, or operational support endpoints.

Workflow communication rules:

- Axon commands carry intent inside stateful workflows, such as post journal, create payment instruction, activate wallet, or retry a reviewed transaction.
- Axon queries or command responses carry bounded immediate decisions needed before money moves, such as wallet status, merchant active state, fraud result, and ledger posting result.
- Axon events represent accepted workflow/domain facts and drive sagas or process managers.
- Kafka events are Avro integration facts published after commit for reporting, notifications, audit streams, analytics, and cross-service projections.
- Kafka events must not be used as request commands for financial correctness. A service that needs another service to make an immediate financial decision must use Axon command/query semantics and fail closed on timeout.

API contracts should be documented with OpenAPI per service.

Kafka contracts should be documented as Avro schema files in a shared contracts area. Services must depend on those schemas instead of hand-building untyped JSON messages.

## Security

Security baseline:

- OAuth2/OIDC with Keycloak.
- JWT validation at the gateway and service layer.
- Role-based access control.
- Service-to-service credentials for internal calls.
- Rate limiting for sensitive endpoints.
- Idempotency key required for money-moving commands.
- Approved KYC required before any wallet transaction.
- Audit logs for financial and admin actions.
- KYC documents stored outside application databases.
- No secrets committed to the repository.

## Observability

Each service should expose:

- Spring Boot Actuator health endpoint.
- Metrics through Micrometer.
- Structured logs with correlation IDs.
- Distributed tracing with OpenTelemetry-compatible instrumentation.

Local development should make room for:

- Grafana.
- Prometheus.
- Loki or another log store.
- Jaeger for local distributed tracing.

## Local Development Topology

Initial Docker Compose services:

- Keycloak.
- Spring Cloud Config Server.
- Eureka Server.
- Axon Server.
- Kafka.
- Schema Registry.
- PostgreSQL.
- MongoDB.
- Redis.
- MinIO.
- Spring Cloud Gateway.
- Core Spring Boot services.
- React Native development environment.
- React admin development server.

## Kubernetes And Helm

FinFlow will support Minikube as the local Kubernetes target after Docker Compose is stable.

Helm chart structure should support:

- Gateway deployment.
- Per-service deployments.
- Spring Cloud Config Server.
- Eureka Server.
- Service configuration through values files.
- Keycloak, Axon Server, Kafka, Schema Registry, PostgreSQL, MongoDB, Redis, and MinIO as local development dependencies or references to externally managed services.
- Ingress for local access.
- Secrets mounted from Kubernetes secrets for local development values.
- Environment-specific values: `values-local.yaml`, `values-minikube.yaml`, and later `values-aws.yaml`.

## AWS Deployment Path

The architecture should be explainable against AWS even before full deployment:

- API entry: AWS API Gateway or ALB plus Spring Cloud Gateway.
- Compute: ECS Fargate for simpler operations, EKS for Kubernetes demonstration.
- Relational database: Amazon RDS for PostgreSQL.
- NoSQL: Amazon DocumentDB or MongoDB Atlas.
- Cache: ElastiCache for Redis.
- Object storage: S3 for KYC documents.
- Secrets: AWS Secrets Manager or SSM Parameter Store.
- Logs and metrics: CloudWatch, OpenTelemetry collector, Prometheus/Grafana option.
- Container registry: ECR.
- CI/CD: GitHub Actions.

## Key Tradeoffs

- Microservices increase operational complexity but match the target role and demonstrate bounded-context ownership.
- Axon and CQRS are powerful but should be applied mainly to financial workflows, not every simple CRUD operation.
- Kafka adds operational weight but creates a strong integration-events story for reporting, notifications, fraud, and analytics.
- Keycloak reduces custom security risk and creates strong interview talking points around OAuth2/OIDC and RBAC.
- PostgreSQL remains the source of truth for financial state because correctness matters more than flexible storage.
- MongoDB is best used for query projections and reporting documents, not wallet balance.
