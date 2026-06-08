# FinFlow Q&A

This file captures the questions that should be answered before implementation or during early milestones. User answers are recorded as decisions; remaining defaults are included so work can proceed without blocking.

## Product Decisions

### 1. Who is the primary demo audience?

Decision: portfolio depth, with the possibility of a real product launch.

Options:

- Technical lead interview.
- Senior full-stack interview.
- Backend-heavy portfolio review.
- Mobile-heavy portfolio review.

Impact:

- A technical lead audience needs clearer roadmap, architecture decisions, team split, and operational tradeoffs.
- A senior full-stack audience needs more complete app flows and less management framing.

### 2. Should wallet access require approved KYC?

Decision: approved KYC is required before any wallet transaction.

Impact:

- Requiring KYC before all money movement is more realistic for regulated finance.
- Allowing low-limit usage before KYC creates a smoother demo.

### 3. Which currency should the MVP use?

Decision: IDR.

Impact:

- IDR matches Indonesian payment/KYC context and common local fintech examples.
- Multi-currency can be added later but should not be part of the first MVP.

### 4. Should the app support merchants in the first version?

Decision: merchants are included in the MVP.

Impact:

- Adding merchants strengthens QR payment and reporting demos.
- Excluding merchants keeps the first release focused on customer wallet and admin operations.

### 5. Should payment gateway simulation resemble a specific provider?

Decision: generic simulated payment provider.

Impact:

- A named provider makes demos easier to understand.
- A generic provider avoids coupling the domain model to one vendor.

## Architecture Decisions

### 6. Should this be a monorepo?

Default: yes.

Impact:

- A monorepo is easier for a portfolio project and local development.
- Separate repos are more enterprise-like but add overhead without much interview value.

### 7. Should every service use CQRS?

Default: no. Use CQRS for financial workflows and operational projections.

Impact:

- Applying CQRS everywhere creates ceremony.
- Applying CQRS selectively shows judgment.

### 8. Should Axon Server be required locally?

Default: yes, alongside Kafka for integration events.

Impact:

- Axon Server gives a clear AxonIQ story and supports event-driven architecture.
- Simpler broker-only setups are easier but less aligned with the requested AxonIQ direction.

### 9. Should MongoDB be mandatory?

Default: yes, but only for projections/reporting/document-shaped reads.

Impact:

- MongoDB demonstrates NoSQL usage cleanly.
- PostgreSQL remains the source of truth for money and workflow state.

### 10. Should service discovery use Eureka or Docker DNS?

Decision: add Eureka as the Spring Cloud service registry.

Impact:

- Docker DNS is simpler and enough for local Compose.
- Eureka demonstrates more Spring Cloud features but can be unnecessary overhead.

### 11. Should Spring Cloud Config be included early?

Decision: include Spring Cloud Config in Phase 0.

Impact:

- Early Config Server demonstrates cloud-native config.
- Delaying it reduces initial bootstrap complexity.

## Frontend Decisions

### 12. React Native framework choice?

Decision: Expo.

Impact:

- Expo accelerates auth, camera, QR, notifications, and demos.
- Bare React Native gives lower-level control but increases setup complexity.

### 13. Admin UI framework?

Decision: Material UI.

Impact:

- Material UI is fast for a professional operations dashboard.
- Ant Design is also strong for enterprise dashboards.

### 14. Mobile state management?

Default: TanStack Query for server state and Zustand for small client state.

Impact:

- Keeps global state small and maintainable.
- Redux Toolkit is acceptable but heavier for this project.

## Operations Decisions

### 15. Local orchestration first target?

Decision: Docker Compose for local development, plus Kubernetes support through Minikube and Helm charts.

Impact:

- Docker Compose is easiest for local demos.
- Kubernetes can be added later through Helm or manifests.

### 16. AWS deployment target?

Decision: Minikube and Helm for Kubernetes locally. AWS deployment remains a later production-readiness guide.

Impact:

- ECS Fargate is easier to explain and operate.
- EKS better demonstrates Kubernetes but increases operational burden.

### 17. Observability stack?

Default: Actuator, Micrometer, OpenTelemetry, Prometheus, Grafana, and a tracing backend.

Impact:

- Strong interview value.
- Should come after core flows work.

## Answers To Provide Next

Answered:

1. Main goal: portfolio depth, with possible real product launch.
2. KYC: required before any wallet transaction.
3. MVP currency: IDR.
4. Mobile framework: Expo.
5. Deployment: Docker Compose locally, Minikube with Helm for Kubernetes.
6. Merchants: included in MVP.
7. Async architecture: Axon Server plus Kafka.
8. Merchant onboarding: full KYB.
9. Admin UI: Material UI.
10. Keycloak setup: realm export plus automated setup script.
11. Local object storage: MinIO.
12. Kafka schemas: Avro with Schema Registry for producer/consumer contracts.
13. Payment gateway simulation: generic provider.
14. Spring Cloud Config: Phase 0.
15. Service discovery: Eureka.
16. Avro compatibility mode: backward.

Contract decision:

- Services must not publish untyped Kafka JSON events.
- Kafka event schemas live in shared Avro contract files.
- Producers serialize with Avro and register/use schemas through Schema Registry.
- Consumers deserialize through Schema Registry and must be idempotent.
- CI should validate schema compatibility so a service cannot send garbage or break consumers silently.
- Schema Registry compatibility mode is backward.

Remaining questions:

1. Should mobile state management stay TanStack Query plus Zustand, or do you prefer Redux Toolkit?
2. Should observability use Grafana Tempo or Jaeger for local tracing?
