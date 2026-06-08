# FinFlow Roadmap

## Delivery Strategy

Build FinFlow in vertical slices. Each milestone should produce demoable software, working tests, and updated docs.

The project should start smaller than the final architecture but keep boundaries clear enough that new services can be added without rewrites.

## Phase 0: Foundation And Repo Bootstrap

Goal: create a working monorepo foundation for local Compose and later Minikube deployment.

Deliverables:

- Root README and documentation.
- Backend workspace structure.
- Mobile app workspace.
- Admin dashboard workspace.
- Docker Compose baseline.
- Helm chart skeleton for Minikube.
- Keycloak automated setup script skeleton.
- Shared contract directory for Avro schemas.
- Spring Cloud Config baseline.
- Eureka service registry baseline.
- Shared coding standards.
- GitHub Actions skeleton.

Recommended first services:

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

Exit criteria:

- `docker compose up` starts infrastructure.
- Gateway health endpoint responds.
- Keycloak realm can issue tokens.
- Keycloak setup script can create or update the local realm, clients, and roles.
- Spring Cloud Config can serve local service configuration.
- Eureka dashboard shows registered backend services.
- Axon Server is reachable.
- Kafka broker is reachable.
- Schema Registry is reachable.
- MinIO is reachable.
- Helm chart can render Kubernetes manifests with `helm template`.

## Phase 1: Identity, Gateway, And Profiles

Goal: establish secure access and user identity mapping.

Deliverables:

- Keycloak realm configuration for FinFlow.
- Automated Keycloak setup script.
- Customer and admin roles.
- Merchant role.
- Spring Cloud Gateway JWT validation.
- User Service with profile creation.
- React Native login flow.
- React admin login flow.

Core flows:

- Customer signs in.
- Merchant signs in.
- Admin signs in.
- Backend maps Keycloak subject to application profile.
- Gateway denies unauthorized requests.

Exit criteria:

- Mobile and admin apps can authenticate.
- Protected backend endpoints reject invalid tokens.
- User profile is created or loaded after first sign-in.

## Phase 2: KYC, Merchant Onboarding, And Admin Operations

Goal: enforce approved onboarding before any wallet transaction.

Deliverables:

- KYC Service.
- Customer KYC submission flow.
- Merchant profile and full KYB onboarding flow.
- KYC document metadata.
- MinIO-backed local object storage.
- Admin dashboard KYC queue.
- Admin approve/reject actions for customers and merchants.
- Audit logs for admin decisions.

Core flows:

- Customer submits KYC.
- Merchant submits full KYB profile and business documents.
- Admin reviews customer and merchant onboarding.
- Customer and merchant account status changes based on decision.
- Wallet transaction access rules reflect approved KYC state.

Exit criteria:

- Admin can approve and reject customer KYC.
- Admin can approve and reject merchant onboarding.
- Customer sees current KYC status.
- Merchant sees current onboarding status.
- Unapproved customers and merchants cannot perform wallet transactions.
- KYC decisions are auditable.

## Phase 3: Wallet And Ledger Core

Goal: implement financial correctness foundation.

Deliverables:

- Wallet Service.
- Ledger Service.
- Wallet creation after approved KYC/onboarding.
- Double-entry ledger model.
- Wallet balance projection.
- Idempotency table for money-moving commands.

Core flows:

- Create wallet.
- Prevent wallet transaction access before KYC approval.
- Post ledger journal.
- Query wallet balance.
- Reject duplicate command by idempotency key.

Exit criteria:

- Every ledger journal balances.
- Wallet balance can be rebuilt from ledger entries.
- Duplicate transfer/top-up requests do not double-credit or double-debit.

## Phase 4: Top-Up, Transfer, And Merchant Payment MVP

Goal: deliver the first complete customer and merchant money movement demo.

Deliverables:

- Transaction Service.
- Payment Service with simulated gateway.
- Generic payment-provider simulation.
- Axon command and event flow for top-up.
- Axon command and event flow for peer transfer.
- Axon command and event flow for merchant QR payment.
- Kafka Avro integration events for transaction, ledger, payment, reporting, and notification consumers.
- Schema Registry validation for published event contracts.
- React Native wallet dashboard.
- React Native top-up screen.
- React Native transfer screen.
- React Native merchant payment screens.
- Transaction history projection.

Core flows:

- Customer tops up wallet.
- Customer transfers to another customer.
- Merchant generates a QR payment request.
- Customer pays merchant by QR.
- Transaction status updates from pending to completed or failed.
- Ledger entries are created for successful movements.

Exit criteria:

- End-to-end demo works from mobile app to backend.
- Merchant QR payment demo works from mobile app to backend.
- Transaction history shows successful and failed records.
- Test suite covers duplicate request and insufficient funds cases.
- Contract tests prevent services from publishing incompatible Kafka events.

## Phase 5: Fraud, Limits, And Account Controls

Goal: demonstrate AML-style thinking without building a full compliance platform.

Deliverables:

- Fraud Service.
- Configurable transaction limits.
- Velocity checks.
- Suspicious activity alerts.
- Account freeze/unfreeze workflow.
- Admin fraud review screen.
- Kafka fraud signal consumers.

Core flows:

- Large or repeated transfer is flagged.
- Admin reviews alert.
- Admin freezes or unfreezes wallet.
- Frozen wallets cannot send money.

Exit criteria:

- Fraud alerts appear in admin dashboard.
- Frozen-wallet restrictions are enforced at backend level.
- Audit log records admin account-control actions.

## Phase 6: QR Payments And Notifications

Goal: deepen mobile-native payment and notification features.

Deliverables:

- Customer QR payment request generation.
- Customer QR scan payment flow.
- Merchant QR payment refinements.
- Notification Service.
- In-app notification center.
- Optional push notification adapter.

Core flows:

- Customer generates QR payment request.
- Another customer scans and pays.
- Customer pays an approved merchant.
- Both customers receive transaction notifications.

Exit criteria:

- QR payment demo works on mobile.
- Notifications are persisted and visible.
- Failed notification delivery does not break money movement.

## Phase 7: Reporting And Observability

Goal: make the platform explainable as an operated system.

Deliverables:

- Reporting Service with MongoDB projections.
- Kafka consumers for reporting projections.
- Avro schema compatibility checks for reporting event consumers.
- Admin charts for transaction volume, failed payments, KYC decisions, and fraud alerts.
- Actuator endpoints.
- Metrics.
- Structured logs.
- Tracing baseline.

Core flows:

- Admin views operational report.
- Developer can trace a transaction across services.
- Health checks show service readiness.

Exit criteria:

- Reports update from domain events.
- Kafka consumers update reporting projections idempotently.
- Logs include correlation IDs.
- Metrics can be scraped locally.

## Phase 8: Production Readiness

Goal: prepare the project for a serious interview walkthrough.

Deliverables:

- GitHub Actions CI.
- Backend unit and integration tests.
- Testcontainers for service/database tests.
- OpenAPI documentation.
- Avro schemas and Schema Registry compatibility checks.
- Docker images.
- Helm chart for Minikube.
- Minikube deployment guide.
- AWS deployment guide.
- Architecture decision records.
- Demo script.

Exit criteria:

- A fresh clone can run the platform locally.
- Helm chart deploys core services to Minikube.
- CI runs tests.
- CI validates OpenAPI and Kafka schema contracts.
- API docs are available.
- Interview demo script completes reliably.

## Suggested Build Order

1. Bootstrap infrastructure.
2. Configure Keycloak.
3. Build gateway auth.
4. Build User Service.
5. Build KYC and Merchant onboarding.
6. Build Wallet and Ledger services.
7. Build Transaction and Payment services.
8. Build React Native wallet and merchant flows.
9. Build React admin dashboard.
10. Add fraud, reporting, observability, and Helm deployment.

## Team Leadership Framing

If describing this as a technical lead project, split ownership this way:

- Mobile engineer: Expo React Native auth, wallet, KYC, merchant, transfer, QR, notifications.
- Web engineer: React admin dashboard with Material UI, KYC/merchant review, monitoring, reporting.
- Backend engineer 1: Gateway, Keycloak integration, User Service.
- Backend engineer 2: Wallet, Ledger, Transaction services.
- Backend engineer 3: Merchant, Payment, Fraud, Notification, Reporting services.
- Platform engineer: Docker, Kafka, Schema Registry, MinIO, Helm, Minikube, CI/CD, observability, AWS deployment guide.

The technical lead owns domain boundaries, API contracts, code review standards, delivery sequencing, risk management, and stakeholder-facing demos.
