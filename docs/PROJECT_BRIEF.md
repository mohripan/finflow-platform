# FinFlow Project Brief

## Purpose

FinFlow is a cloud-native digital wallet platform designed as a deep portfolio project with a realistic path toward product launch. It is also shaped for a technical lead or senior full-stack role requiring Java, React Native, ReactJS, REST APIs, AWS, API Gateway, cloud-native engineering, and financial-system familiarity.

The project should demonstrate that the system owner can design, build, test, operate, and explain a production-style financial platform, not just implement CRUD screens.

## Product Scope

FinFlow will include three primary surfaces:

- Customer mobile app using React Native and TypeScript.
- Merchant-facing MVP flows inside the mobile experience or a dedicated merchant profile area.
- Admin operations dashboard using ReactJS and TypeScript.
- Spring Boot microservices backend using CQRS, event-driven workflows, and cloud-native infrastructure.

## Core User Journeys

### Customer

1. Register and sign in through Keycloak.
2. Complete profile setup.
3. Submit KYC data and documents.
4. Wait for KYC approval before wallet transactions are enabled.
5. View wallet balance.
6. Top up using a simulated payment gateway.
7. Transfer money to another user.
8. Generate and scan QR payments.
9. Pay a merchant by QR.
10. View transaction history and receipts.
11. Receive notifications.
12. Freeze or secure the account when suspicious activity is detected.

### Merchant

1. Register and sign in through Keycloak.
2. Complete full KYB business verification and owner identity linkage.
3. Receive approval from admin.
4. Generate a merchant QR payment request.
5. View incoming payments.
6. View settlement-style transaction reports.

### Admin

1. Sign in through Keycloak with admin role.
2. Review pending KYC submissions.
3. Approve or reject KYC cases.
4. Monitor transactions.
5. Inspect suspicious activity alerts.
6. Freeze or unfreeze accounts.
7. View operational and financial reports.
8. Audit important user and admin actions.

## Non-Goals For The First MVP

- Real external banking integration.
- Real money movement.
- Full AML case management.
- Real biometric identity verification.
- Native iOS/Android custom modules unless needed.
- Production Kubernetes deployment before local Docker Compose is stable.

## Success Criteria

The MVP is successful when a demo can show:

- A customer registers, passes a simple KYC workflow, receives a wallet, tops up, and transfers money.
- A merchant is onboarded, approved, and can receive a QR payment.
- The ledger records immutable double-entry movements.
- Duplicate payment or transfer requests are rejected or safely replayed through idempotency keys.
- Admin users can inspect KYC, transactions, account status, and reports.
- Services communicate through explicit REST APIs, Axon commands/events, and Kafka integration events.
- Kafka integration events use Avro schemas registered in Schema Registry so producers and consumers share enforceable contracts.
- The platform runs locally through Docker Compose.
- The platform has a Minikube deployment path through Helm charts.
- Architecture, API contracts, operational concerns, and tradeoffs are documented.

## Primary Interview Talking Points

- Why Keycloak was used for centralized identity, RBAC, OAuth2, and token issuance.
- Why wallet balance and ledger records are separated.
- How CQRS isolates write-side commands from read-side query models.
- How Axon supports command handling, events, sagas/process managers, and auditability.
- How idempotency, database transactions, and the outbox/event model reduce financial correctness risks.
- How the platform could be deployed on AWS with API Gateway, ECS/EKS, RDS, ElastiCache, S3, CloudWatch, and Secrets Manager.
- How work can be split across a team by bounded context and delivered by milestones.
