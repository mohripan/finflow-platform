# FinFlow Service Boundaries

## Purpose

This document defines what each service owns, what it may decide, what it may call, and what it must not know. It should be used before implementation to prevent service sprawl, cross-service database access, duplicated business rules, and unclear ownership.

## Boundary Principles

1. A service owns its database schema and nobody else reads or writes it directly.
2. A service is the only authority for state transitions of its owned models.
3. Cross-service joins are forbidden.
4. Public REST calls are for client/admin access through the gateway. Internal REST is allowed only for non-financial queries and operational support APIs.
5. Multi-step business workflows and financial decisions use Axon commands, events, queries, and sagas/process managers.
6. Broad integration, reporting, notification fan-out, and analytics use Kafka Avro events.
7. Kafka events are not the source of truth for money.
8. Ledger Service is the accounting source of truth.
9. Wallet balance is a projection, not the accounting source of truth.
10. Admin actions must be audited by the service that owns the affected state.

## Service Map

| Service | Primary Responsibility | Primary Store |
| --- | --- | --- |
| Gateway | Public API routing, token validation, rate limiting | Redis for gateway concerns |
| Config Server | Centralized service configuration | Git/filesystem config |
| Discovery Server | Eureka service registry | In-memory registry |
| User Service | User profile and customer account lifecycle | PostgreSQL |
| KYC Service | Customer KYC applications and documents | PostgreSQL + MinIO |
| Merchant Service | Merchant profile, KYB, withdrawal destination | PostgreSQL + MinIO |
| Wallet Service | Wallet state and balance projections | PostgreSQL |
| Ledger Service | Ledger accounts, journals, entries | PostgreSQL |
| Transaction Service | Business transaction lifecycle and QR/refund workflows | PostgreSQL |
| Payment Service | Generic payment and payout simulation | PostgreSQL |
| Fraud Service | Risk decisions, fraud alerts, admin retry decisions | PostgreSQL |
| Notification Service | In-app notification delivery | MongoDB or PostgreSQL |
| Reporting Service | Admin and merchant read projections | MongoDB |

## Gateway

Owns:

- Public route map.
- JWT validation configuration.
- Rate limiting.
- Correlation ID creation/propagation.

Does not own:

- Authentication credentials.
- Domain authorization decisions beyond coarse route/role checks.
- Business validation.

Allowed dependencies:

- Keycloak JWKs/token introspection where needed.
- Redis for rate limiting.
- Eureka for service lookup.

Rules:

- Gateway may reject unauthenticated or role-incompatible requests.
- Gateway must not decide KYC/KYB, wallet, transaction, ledger, refund, or fraud outcomes.
- Gateway must forward correlation IDs to downstream services.

## Config Server

Owns:

- Shared runtime configuration.
- Environment-specific config defaults.

Does not own:

- Secrets that should live in secret stores.
- Business data.

Rules:

- Config Server is included in Phase 0.
- Local secrets may use development-only values, but production-like docs should point to Kubernetes secrets or cloud secret stores.

## Discovery Server

Owns:

- Eureka service registration and discovery.

Does not own:

- Load-balancing business rules.
- Service health decisions beyond registry status.

Rules:

- Services register themselves with Eureka in local and Minikube environments.
- Kubernetes DNS may still exist, but application service discovery should demonstrate Eureka as selected architecture.

## User Service

Owns:

- `UserProfile`
- `CustomerAccount`
- Mapping between Keycloak subject and FinFlow user profile.
- Customer account lifecycle status that is not owned by KYC or Wallet.

Decides:

- Whether a Keycloak subject has an application profile.
- Whether user profile status is active, suspended, or closed.
- Whether a customer account exists.

Does not decide:

- KYC approval.
- Wallet activation success.
- Merchant approval.
- Financial transaction validity.

Synchronous APIs:

- Get current user profile.
- Create/load profile after login.
- Get customer account status.
- Admin view/update user profile status.

Consumes:

- `KycApproved`
- `KycRejected`
- `KycLocked`
- `WalletActivated`
- `WalletFrozen`
- `WalletClosed`

Publishes:

- `UserProfileCreated`
- `CustomerAccountCreated`
- `CustomerAccountSuspended`
- `CustomerAccountClosed`

Edge-case ownership:

- If Keycloak user exists but profile does not, User Service creates profile idempotently.
- If KYC is approved but wallet activation fails, User Service must not mark wallet active until Wallet Service confirms.

Forbidden:

- Storing passwords.
- Reading Keycloak database.
- Reading KYC or Wallet databases.

## KYC Service

Owns:

- `KycApplication`
- `KycDocument`
- Customer KYC decision history.
- Customer KYC rejection lock state.

Decides:

- Customer KYC submission validity.
- KYC approval, rejection, resubmission required, lock, unlock.
- Whether KYC document metadata is accepted or rejected.

Does not decide:

- Customer wallet activation.
- Merchant KYB approval.
- Transaction authorization.

Synchronous APIs:

- Submit KYC.
- Upload/get KYC document metadata.
- Get current user's KYC status.
- Admin list/review KYC applications.
- Admin unlock locked KYC.

Consumes:

- `UserProfileCreated`
- `CustomerAccountCreated`

Publishes:

- `KycSubmitted`
- `KycApproved`
- `KycRejected`
- `KycResubmissionRequired`
- `KycLocked`
- `KycUnlocked`

Edge-case ownership:

- Duplicate national identity number is flagged inside KYC Service.
- Rejection count increments only on rejection, not resubmission request.
- Default lock threshold is 3 rejections.

Forbidden:

- Creating wallets directly.
- Writing user account status directly.
- Storing raw document bytes in PostgreSQL.

## Merchant Service

Owns:

- `Merchant`
- `KybApplication`
- `KybDocument`
- `WithdrawalDestination`
- Merchant KYB decision history.
- Merchant rejection lock state.

Decides:

- Merchant profile lifecycle.
- KYB approval, rejection, resubmission required, lock, unlock.
- Withdrawal destination verification status.
- Whether merchant can generate payment requests from merchant-state perspective.

Does not decide:

- Customer KYC approval.
- Wallet balance sufficiency.
- Ledger posting.
- Transaction completion.

Synchronous APIs:

- Create merchant profile.
- Submit KYB.
- Upload/get KYB document metadata.
- Get merchant status.
- Manage withdrawal destination.
- Admin list/review merchant KYB applications.
- Admin suspend/reactivate merchant.

Consumes:

- `KycApproved`
- `KycLocked`
- `KycRejected`
- `WalletActivated`
- `MerchantWalletCreated`

Publishes:

- `MerchantRegistered`
- `KybSubmitted`
- `KybApproved`
- `KybRejected`
- `KybResubmissionRequired`
- `KybLocked`
- `KybUnlocked`
- `MerchantActivated`
- `MerchantSuspended`
- `WithdrawalDestinationVerified`

Edge-case ownership:

- Merchant owner must have approved KYC before KYB approval.
- If owner KYC becomes locked/revoked later, Merchant Service suspends or flags merchant.
- Business registration number reuse is flagged for admin review.

Forbidden:

- Reading KYC database directly.
- Posting ledger entries.
- Moving money.

## Wallet Service

Owns:

- `Wallet`
- `BalanceProjection`
- Wallet state: pending activation, active, frozen, closed.
- Available and pending balance projection.

Decides:

- Whether wallet is active/frozen/closed.
- Whether projected available balance is sufficient for display and non-authoritative pre-checks.
- How balance projection changes from ledger events.

Does not decide:

- Whether ledger journal is valid.
- Whether a debit account has sufficient authoritative posted balance.
- Whether transaction should complete.
- KYC/KYB approval.
- Fraud outcome.

Synchronous APIs:

- Get wallet by owner.
- Get available and pending balance.
- Admin freeze/unfreeze wallet.
- Internal wallet status validation.

Consumes:

- `KycApproved`
- `KybApproved`
- `LedgerJournalPosted`
- `MerchantWithdrawalPending`
- `MerchantWithdrawalCompleted`
- `MerchantWithdrawalFailed`
- `RefundCompleted`

Publishes:

- `WalletCreated`
- `MerchantWalletCreated`
- `WalletActivated`
- `WalletFrozen`
- `WalletUnfrozen`
- `WalletClosed`
- `BalanceProjectionUpdated`

Edge-case ownership:

- Pending withdrawal reduces available balance and increases pending balance.
- Failed payout releases pending back to available.
- Refund credits can increase balance even if wallet outgoing transactions are frozen.
- Instant transfer and merchant payment flows do not create wallet-only reservations. Wallet Service may perform status and projection pre-checks, but the authoritative insufficient-funds decision is made by Ledger Service during journal posting.
- Pending withdrawal is represented by a posted ledger movement into payout clearing and then projected as pending balance.

Forbidden:

- Treating projection as ledger source of truth.
- Posting ledger journals.
- Completing transactions.

## Ledger Service

Owns:

- `LedgerAccount`
- `LedgerJournal`
- `LedgerEntry`
- Accounting invariants.

Decides:

- Whether a journal balances.
- Whether a journal can be posted.
- Whether duplicate journal posting should be rejected or idempotently returned.
- Whether reversal journal is valid.
- Whether debit-side ledger accounts have sufficient available posted balance at journal-post time.

Does not decide:

- Business transaction approval.
- Fraud decision.
- KYC/KYB approval.
- Payment provider result.

Synchronous APIs:

- Create ledger account.
- Post journal.
- Get journal by reference.
- Rebuild/check account balance for reconciliation.

Consumes:

- Commands from transaction workflows to post journals.
- `WalletCreated`
- `MerchantActivated`

Publishes:

- `LedgerAccountCreated`
- `LedgerJournalPosted`
- `LedgerJournalRejected`
- `LedgerJournalReversed`

Edge-case ownership:

- Flat fee must post to fee revenue account.
- Refund uses reversal entries and does not mutate original journal.
- Duplicate journal post for same transaction step is idempotent or rejected deterministically.
- Failed merchant payout must be corrected through a reversal journal from payout clearing back to merchant business balance.
- Ledger Service serializes journal posting per affected ledger account, using database row locks, an equivalent pessimistic lock, or serializable transaction semantics so two concurrent debits cannot overspend the same account.
- Ledger account balance used for posting is derived from posted ledger entries inside the same transaction boundary as the journal insert, not from Wallet Service projections or cached balances.
- System ledger accounts for payment clearing, payout clearing, fee revenue, and suspense are bootstrapped idempotently per currency before money movement is enabled.

Forbidden:

- Storing negative ledger-entry amounts.
- Allowing unbalanced journals.
- Deleting or mutating posted entries.

## Transaction Service

Owns:

- `Transaction`
- `IdempotencyRecord` for transaction commands.
- `QrPaymentRequest`
- `TransferQrToken`
- `RefundRequest`
- Transaction state machine.

Decides:

- Transaction lifecycle state.
- Idempotency behavior for transaction commands.
- QR request validity.
- Whether to request fraud check, payment simulation, payout simulation, wallet validation, or ledger posting.
- Whether refund request enters admin approval queue.

Does not decide:

- KYC/KYB final approval.
- Fraud rules.
- Ledger accounting validity.
- Payment/payout callback truth.
- Wallet projection calculation.

Synchronous APIs:

- Create top-up.
- Confirm simulated top-up where applicable.
- Generate transfer QR.
- Pay transfer QR.
- Generate merchant payment QR.
- Pay merchant QR.
- Request merchant withdrawal.
- Request refund.
- Admin approve/reject refund.
- Get transaction history.

Consumes:

- `PaymentPaid`
- `PaymentFailed`
- `PaymentExpired`
- `PayoutCompleted`
- `PayoutFailed`
- `FraudAllowed`
- `FraudReviewRequired`
- `FraudBlocked`
- `FraudReviewRetryRequested`
- `LedgerJournalPosted`
- `LedgerJournalRejected`

Publishes:

- `TopUpRequested`
- `TransferRequested`
- `MerchantPaymentRequested`
- `MerchantWithdrawalRequested`
- `MerchantWithdrawalPending`
- `MerchantWithdrawalCompleted`
- `MerchantWithdrawalFailed`
- `RefundRequested`
- `RefundApproved`
- `RefundRejected`
- `RefundCompleted`
- `TransactionCompleted`
- `TransactionFailed`
- `TransactionRejected`

Edge-case ownership:

- Same idempotency key and same request returns original result.
- Same idempotency key and different request is rejected.
- Admin fraud retry keeps original transaction reference but re-runs validations.
- QR payment cannot be paid twice.
- Merchant fixed-amount QR cannot be edited after creation; create a new QR instead.

Forbidden:

- Posting ledger rows directly.
- Mutating payment instruction state directly.
- Reading other service databases.

## Payment Service

Owns:

- `PaymentInstruction`
- `PayoutInstruction`
- Generic payment simulation.
- Generic payout callback simulation.

Decides:

- Simulated payment status.
- Simulated payout status.
- Whether duplicate callbacks are idempotently ignored.

Does not decide:

- Whether user can top up.
- Whether merchant withdrawal is allowed.
- Ledger posting.
- Transaction completion beyond publishing payment/payout result.

Synchronous APIs:

- Create payment instruction.
- Simulate payment success/failure/expiry.
- Create payout instruction.
- Simulate payout callback success/failure.

Consumes:

- `TopUpRequested`
- `MerchantWithdrawalRequested`

Publishes:

- `PaymentInstructionCreated`
- `PaymentPaid`
- `PaymentFailed`
- `PaymentExpired`
- `PayoutInstructionCreated`
- `PayoutCompleted`
- `PayoutFailed`

Edge-case ownership:

- Duplicate payout callbacks are idempotent.
- Payment success after expiry is rejected or ignored based on instruction state.

Forbidden:

- Updating wallet balance.
- Posting ledger entries.
- Making provider-specific assumptions in transaction models.

## Fraud Service

Owns:

- `FraudAlert`
- Fraud rules.
- Fraud review resolution state.

Decides:

- `ALLOW`, `REVIEW`, or `BLOCK`.
- Whether a fraud alert is open, resolved, retried, or dismissed.
- Whether admin retry request is allowed after review resolution.

Does not decide:

- Ledger posting.
- Payment provider result.
- KYC/KYB approval.
- Wallet balance.

Synchronous APIs:

- Evaluate transaction risk.
- Admin list/review fraud alerts.
- Admin resolve alert.
- Admin request retry for blocked reviewed transaction.

Consumes:

- `TransactionInitiated`
- `MerchantPaymentRequested`
- `MerchantWithdrawalRequested`
- `RefundRequested`
- `TransactionCompleted`
- `TransactionFailed`

Publishes:

- `FraudAllowed`
- `FraudReviewRequired`
- `FraudBlocked`
- `FraudAlertResolved`
- `FraudReviewRetryRequested`

Edge-case ownership:

- `REVIEW` blocks transaction in MVP.
- Review-blocked transaction posts no ledger entries.
- Retry must not loop automatically if it triggers another rule.

Forbidden:

- Retrying transaction directly by writing Transaction Service database.
- Posting ledger entries.

## Notification Service

Owns:

- `Notification`
- Notification templates.
- Delivery status.

Decides:

- Which user-facing notification is created from events.
- Delivery retry behavior.

Does not decide:

- Business transaction outcome.
- KYC/KYB decision.
- Fraud decision.

Synchronous APIs:

- Get recipient notification inbox.
- Mark notification as read.

Consumes:

- KYC/KYB decision events.
- Transaction events.
- Payment/payout events.
- Refund events.
- Fraud alert events.
- Wallet freeze/unfreeze events.

Publishes:

- `NotificationCreated`
- `NotificationSent`
- `NotificationFailed`
- `NotificationRead`

Edge-case ownership:

- Notification failure must never roll back money movement.
- Duplicate consumed events must not create duplicate visible notifications.

Forbidden:

- Calling transaction services to change business state.
- Treating notification delivery as transactional requirement.

## Reporting Service

Owns:

- MongoDB reporting projections.
- Admin dashboards.
- Merchant settlement summaries.
- Fee revenue summaries.

Decides:

- Projection shape.
- Idempotent event consumption.
- Report aggregation windows.

Does not decide:

- Source-of-truth transaction, ledger, KYC, KYB, fraud, or payment state.

Synchronous APIs:

- Admin operational reports.
- Merchant payment reports.
- Merchant withdrawal reports.
- Fee revenue reports.

Consumes:

- KYC events.
- KYB events.
- Transaction events.
- Ledger events.
- Payment/payout events.
- Refund events.
- Fraud events.
- Notification delivery metrics where useful.

Publishes:

- Usually none for MVP.

Edge-case ownership:

- Missing or duplicate events are handled idempotently by event ID.
- Projection rebuild is supported where practical.

Forbidden:

- Reading source service databases directly.
- Serving reports as financial source of truth.

## Audit Responsibility

Audit can be implemented as local audit tables per owning service first. A dedicated Audit Service can be added later if needed.

Each service audits its own sensitive state changes:

- KYC Service audits KYC decisions.
- Merchant Service audits KYB and merchant status decisions.
- Wallet Service audits freeze/unfreeze/close.
- Transaction Service audits refund approval, idempotency conflicts, and transaction admin actions.
- Fraud Service audits fraud resolution and retry requests.
- Ledger Service audits journal posting and reversal metadata through immutable journals.

Rules:

- Audit logs are append-only.
- Audit records include actor, role, action, target, reason, timestamp, and correlation ID.
- Services may publish audit Kafka events for centralized reporting, but local audit remains source of truth for that service's state change.

## Workflow Ownership

### Customer Onboarding

Coordinator:

- KYC approval event triggers wallet activation workflow.

Service responsibilities:

- User Service owns profile/customer account.
- KYC Service owns KYC decision.
- Wallet Service owns wallet creation/activation.
- Ledger Service owns wallet ledger account creation.

### Merchant Onboarding

Coordinator:

- Merchant onboarding saga/process manager.

Service responsibilities:

- Merchant Service owns merchant and KYB state.
- KYC Service provides owner KYC status via event/API.
- Wallet Service owns merchant wallet creation.
- Ledger Service owns merchant ledger account.

### Top-Up

Coordinator:

- Transaction Service saga/process manager.

Service responsibilities:

- Transaction Service owns lifecycle and idempotency.
- Fraud Service evaluates risk where configured.
- Payment Service simulates payment.
- Ledger Service posts top-up journal.
- Wallet Service updates projection from ledger event.
- Notification Service informs customer.
- Reporting Service consumes events.

### Transfer

Coordinator:

- Transaction Service saga/process manager.

Service responsibilities:

- Transaction Service owns transfer request and QR token.
- Wallet Service validates wallet state and may provide a projection pre-check.
- Fraud Service evaluates risk.
- Ledger Service posts the transfer journal and makes the authoritative balance sufficiency decision under account-level serialization.
- Notification and Reporting consume events.

### Merchant Payment

Coordinator:

- Transaction Service saga/process manager.

Service responsibilities:

- Merchant Service validates merchant active state.
- Wallet Service validates customer wallet state and may provide a projection pre-check.
- Fraud Service evaluates risk.
- Ledger Service posts customer debit, merchant net credit, and fee revenue credit, and makes the authoritative balance sufficiency decision under account-level serialization.
- Transaction Service marks payment complete.

### Merchant Withdrawal

Coordinator:

- Transaction Service saga/process manager.

Service responsibilities:

- Merchant Service validates merchant and withdrawal destination.
- Wallet Service exposes available/pending projection.
- Ledger Service moves merchant balance to payout clearing.
- Payment Service emits simulated payout callback.
- Wallet Service updates pending/available projection from ledger/payout events.

### Refund

Coordinator:

- Transaction Service owns refund request and admin approval state.

Service responsibilities:

- Merchant initiates refund request.
- Admin approves/rejects in Transaction Service workflow.
- Fraud Service may evaluate refund risk.
- Ledger Service posts reversal only after approval. A full merchant payment refund reverses the customer gross payment, the merchant net credit, and the fee revenue credit from the original payment journal.
- Wallet Service updates customer and merchant projections.

### Fraud Review Retry

Coordinator:

- Fraud Service owns alert resolution.
- Transaction Service owns retry execution.

Service responsibilities:

- Fraud Service publishes `FraudReviewRetryRequested`.
- Transaction Service reloads original transaction intent.
- Transaction Service re-runs current validations.
- Ledger posting still happens at most once.
- Audit is written in both Fraud Service and Transaction Service where relevant.

## Workflow Communication Matrix

| Need | Mechanism | Notes |
| --- | --- | --- |
| User/mobile/admin request to owning service | REST through Gateway | Gateway handles authentication and coarse role checks; services enforce business rules. |
| Immediate financial workflow command between bounded contexts | Axon command/query with bounded response | Use for ledger posting, fraud evaluation, wallet status validation, merchant active validation, payout/payment instruction creation, and retry decisions. |
| Operational or non-financial service lookup | Authenticated internal REST | Use only when the result does not decide whether money moves. |
| Stateful multi-step coordination | Axon saga/process manager | Use for top-up, transfer, merchant payment, withdrawal, refund, onboarding, and fraud retry. |
| Committed integration fact | Kafka Avro event through outbox | Use after local commit for reporting, notifications, audit streams, and projections. |
| Query/read projection update | Kafka Avro event consumer | Consumers must be idempotent and must not treat projections as source of truth. |

Kafka events are not command messages. They describe committed facts after an owning service has made and persisted a decision.

## Synchronous Call Rules

Allowed synchronous calls:

- Gateway to backend services.
- Service to Keycloak metadata/JWKs.
- Transaction Service to Wallet Service through Axon query/command for current wallet status validation.
- Transaction Service to Merchant Service through Axon query/command for merchant active validation.
- Transaction Service to Fraud Service through Axon command/query for risk evaluation.
- Transaction Service to Ledger Service through Axon command for journal posting.
- Admin UI through Gateway to owning services.

Avoid synchronous calls:

- Reporting to source services.
- Notification to source services.
- Ledger to other services for business context.
- Wallet to Transaction Service.

Rule of thumb:

- If the caller needs an immediate decision to continue a financial command, use Axon command/query semantics and fail closed on timeout or unavailable dependency.
- If the caller is building a read model, use Kafka.
- If the caller is coordinating a stateful workflow, use Axon command/event/saga patterns.

## Data Duplication Rules

Allowed duplication:

- Read projections in Reporting Service.
- Balance projections in Wallet Service derived from ledger events.
- Notification snapshots.
- Denormalized admin dashboard views.

Forbidden duplication:

- Copying wallet balance as independent source of truth.
- Copying KYC/KYB approval state and treating it as authoritative without event freshness rules.
- Duplicating ledger entries outside Ledger Service as financial truth.

## Boundary Decisions

These decisions are fixed before implementation:

1. KYB review lives in Merchant Service. KYC Service owns personal identity verification only. A broader Verification Service may be introduced later only through an ADR and migration plan.
2. Audit remains local and mandatory per owning service from the first implementation. A centralized audit stream is published through Kafka for search/reporting, but it is not the audit source of truth.
3. Fraud risk evaluation for money-moving workflows uses Axon command/query semantics with a bounded response before ledger posting. The workflow fails closed on timeout or unavailable Fraud Service.
4. Merchant active validation during payment is authoritative through Merchant Service at confirmation time. Event-cached merchant state may be used only for UI prefill and non-authoritative pre-checks.
5. Long-running customer payment flows must use explicit ledger-backed authorization or clearing-account holds. Wallet-only holds are forbidden. This is not needed for fixed QR payment in the first implementation because payment confirmation posts immediately or fails.
