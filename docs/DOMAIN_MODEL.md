# FinFlow Domain Model

## Purpose

This document defines the core domain models before implementation. It is intentionally explicit about ownership, timestamps, state transitions, identifiers, invariants, and edge cases so services do not invent conflicting models later.

FinFlow uses PostgreSQL as the source of truth for transactional state, MongoDB for read projections/reporting, MinIO for document objects, Axon for command/event workflows, and Kafka/Avro for integration events.

## Global Modeling Rules

### Common Fields

Every persistent model must include:

- `id`: internal UUID primary identifier.
- `createdAt`: UTC timestamp when the record was created.
- `updatedAt`: UTC timestamp when the record was last changed.

Recommended for most mutable business records:

- `version`: optimistic-lock integer or aggregate version.
- `status`: explicit lifecycle state where applicable.
- `createdBy`: actor ID or system actor that created the record.
- `updatedBy`: actor ID or system actor that last changed the record.
- `deletedAt`: nullable UTC timestamp only for soft-deleteable non-financial records.

Financial and audit records should not use destructive deletes.

### Time And Money

- Store timestamps in UTC.
- Use `Instant` in Java and ISO-8601 strings at API boundaries.
- Store money as integer minor units, for example `amountMinor = 100000` for IDR 100,000.
- Store currency as ISO code, MVP default `IDR`.
- Do not use floating-point types for money.
- Use explicit `Money` value objects in code where practical:

```text
amountMinor: long
currency: string
```

### Identifiers

Use separate identifiers for separate concerns:

- `id`: internal database UUID.
- `publicId`: stable external-safe identifier shown in APIs or receipts.
- `keycloakSubjectId`: identity provider user ID.
- `correlationId`: request/workflow trace ID.
- `causationId`: event or command ID that caused a change.
- `idempotencyKey`: client-provided key for money-moving commands.

Never expose database sequence IDs.

### Auditability

For regulated-style workflows:

- Keep decision history append-only.
- Store admin decision reason.
- Store actor and role.
- Store correlation ID.
- Store before/after status when useful.
- Never mutate ledger entries to correct financial behavior.

## Service Ownership

| Model | Owning Service | Source Of Truth |
| --- | --- | --- |
| UserProfile | User Service | PostgreSQL |
| CustomerAccount | User Service | PostgreSQL |
| KycApplication | KYC Service | PostgreSQL |
| KycDocument | KYC Service | PostgreSQL + MinIO |
| Merchant | Merchant Service | PostgreSQL |
| KybApplication | Merchant Service | PostgreSQL |
| KybDocument | Merchant Service | PostgreSQL + MinIO |
| Wallet | Wallet Service | PostgreSQL |
| BalanceProjection | Wallet Service | PostgreSQL |
| LedgerAccount | Ledger Service | PostgreSQL |
| LedgerJournal | Ledger Service | PostgreSQL |
| LedgerEntry | Ledger Service | PostgreSQL |
| Transaction | Transaction Service | PostgreSQL |
| IdempotencyRecord | Transaction Service per command boundary | PostgreSQL |
| PaymentInstruction | Payment Service | PostgreSQL |
| PayoutInstruction | Payment Service | PostgreSQL |
| QrPaymentRequest | Transaction Service | PostgreSQL |
| TransferQrToken | Transaction Service | PostgreSQL |
| RefundRequest | Transaction Service | PostgreSQL |
| FraudAlert | Fraud Service | PostgreSQL |
| Notification | Notification Service | MongoDB or PostgreSQL |
| AuditLog | Audit/Affected Service | PostgreSQL |
| ReportingProjection | Reporting Service | MongoDB |

Cross-service database joins are not allowed. Services communicate through APIs, Axon workflows, and Kafka integration events.

## Shared Value Objects

### Money

Fields:

- `amountMinor`
- `currency`

Rules:

- `amountMinor` must be greater than or equal to zero unless the value object is explicitly used in ledger-entry direction logic.
- Currency must be `IDR` in MVP.
- Arithmetic must preserve integer minor units.

### ActorRef

Fields:

- `actorId`
- `actorType`: `CUSTOMER`, `MERCHANT_OWNER`, `ADMIN`, `SYSTEM`, `SERVICE`
- `role`

Rules:

- Required for admin decisions and audit logs.
- `SYSTEM` is allowed for workflow-generated actions.

### DocumentObjectRef

Fields:

- `bucket`
- `objectKey`
- `contentType`
- `sizeBytes`
- `checksum`

Rules:

- Object contents live in MinIO locally or S3-compatible storage later.
- PostgreSQL stores only metadata and object references.

## User Service Models

### UserProfile

Represents an application user linked to Keycloak.

Fields:

- `id`
- `publicId`
- `keycloakSubjectId`
- `email`
- `phoneNumber`
- `displayName`
- `preferredLanguage`
- `status`: `ACTIVE`, `SUSPENDED`, `CLOSED`
- `createdAt`
- `updatedAt`
- `version`

Indexes:

- Unique `keycloakSubjectId`.
- Unique nullable `email`.
- Unique nullable `phoneNumber`.

Rules:

- Passwords are never stored in FinFlow services.
- A user can own one customer account.
- A user can own zero or more merchant accounts.
- Suspended users cannot initiate onboarding or money movement.

Edge cases:

- Keycloak user exists but profile creation failed: create profile idempotently on next login.
- Email or phone changes in Keycloak: synchronize through explicit profile update, not silently during financial commands.

### CustomerAccount

Represents customer lifecycle state.

Fields:

- `id`
- `userProfileId`
- `status`: `REGISTERED`, `KYC_SUBMITTED`, `KYC_APPROVED`, `KYC_REJECTED`, `KYC_RESUBMISSION_REQUIRED`, `KYC_LOCKED`, `WALLET_ACTIVE`, `FROZEN`, `CLOSED`
- `kycApplicationId`
- `walletId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Customer cannot transact unless KYC is approved and wallet is active.
- `FROZEN` blocks outgoing money movement.
- `CLOSED` blocks ordinary transactions.

Edge cases:

- Customer is approved but wallet creation fails: status remains `KYC_APPROVED`; wallet activation can be retried idempotently.
- Customer is frozen during a pending withdrawal/refund: outgoing commands stop; refund credits may still be allowed.

## KYC Service Models

### KycApplication

Represents personal identity verification.

Fields:

- `id`
- `publicId`
- `userProfileId`
- `legalName`
- `dateOfBirth`
- `nationalIdentityNumberHash`
- `phoneNumber`
- `address`
- `status`: `NOT_SUBMITTED`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `RESUBMISSION_REQUIRED`, `LOCKED`
- `rejectionCount`
- `submittedAt`
- `reviewedAt`
- `reviewedBy`
- `reviewReason`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Store hash or encrypted representation for sensitive identity numbers.
- Default rejection lock limit is 3.
- `LOCKED` can only be unlocked by admin/compliance action.
- Approved KYC is required before wallet activation.

Edge cases:

- Duplicate identity number: flag for admin review; do not auto-approve.
- Resubmission after rejection: create new document versions and append decision history.
- Admin approves stale application while user resubmits: reject stale decision through version check.

### KycDocument

Represents uploaded KYC document metadata.

Fields:

- `id`
- `kycApplicationId`
- `documentType`: `IDENTITY_CARD`, `SELFIE`, `ADDRESS_PROOF`, `OTHER`
- `objectRef`
- `status`: `UPLOADED`, `ACCEPTED`, `REJECTED`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Document bytes are stored in MinIO/S3-compatible storage.
- Document metadata is retained even when application is rejected.
- Rejected document requires reason in decision history.

## Merchant Service Models

### Merchant

Represents a business account.

Fields:

- `id`
- `publicId`
- `ownerUserProfileId`
- `merchantName`
- `businessCategory`
- `businessType`: `INDIVIDUAL`, `CV`, `PT`, `COOPERATIVE`, `OTHER`
- `status`: `REGISTERED`, `KYB_SUBMITTED`, `KYB_APPROVED`, `ACTIVE`, `SUSPENDED`, `CLOSED`
- `kybApplicationId`
- `businessWalletId`
- `withdrawalDestinationId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Owner must have approved KYC before KYB can be approved.
- Merchant cannot receive payments unless `ACTIVE`.
- Suspended merchant cannot generate payment QR or withdraw.

Edge cases:

- Owner KYC revoked/frozen after merchant approval: merchant should be suspended pending review.
- Merchant name change after approval: treat as profile-change request requiring admin review.

### KybApplication

Represents merchant business verification.

Fields:

- `id`
- `publicId`
- `merchantId`
- `ownerUserProfileId`
- `businessName`
- `businessCategory`
- `businessType`
- `businessRegistrationNumber`
- `taxNumber`
- `businessAddress`
- `status`: `NOT_SUBMITTED`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `RESUBMISSION_REQUIRED`, `LOCKED`
- `rejectionCount`
- `submittedAt`
- `reviewedAt`
- `reviewedBy`
- `reviewReason`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Default rejection lock limit is 3.
- Admin approval activates merchant only after wallet/ledger account setup succeeds.
- KYB document history is append-only.

Edge cases:

- Business registration number reused by another merchant: flag for admin review.
- Owner changes: require new KYB or admin approval.

### WithdrawalDestination

Represents merchant payout destination.

Fields:

- `id`
- `merchantId`
- `destinationType`: `BANK_ACCOUNT`, `EXTERNAL_WALLET`
- `accountHolderName`
- `bankCode`
- `accountNumberMasked`
- `accountNumberHash`
- `status`: `PENDING_VERIFICATION`, `VERIFIED`, `REJECTED`, `DISABLED`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Withdrawals require verified destination.
- Store masked and hashed account number, not raw value.

## Wallet Service Models

### Wallet

Represents a customer or merchant balance container.

Fields:

- `id`
- `publicId`
- `ownerType`: `CUSTOMER`, `MERCHANT`
- `ownerId`
- `ledgerAccountId`
- `currency`
- `status`: `PENDING_ACTIVATION`, `ACTIVE`, `FROZEN`, `CLOSED`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- One active IDR wallet per customer in MVP.
- One active IDR business wallet per merchant in MVP.
- Wallet status gates outgoing transactions.

Edge cases:

- Ledger account exists but wallet activation failed: retry activation idempotently.
- Wallet frozen after QR generation: payment validation must re-check wallet state at confirmation time.

### BalanceProjection

Represents wallet read model.

Fields:

- `id`
- `walletId`
- `availableBalanceMinor`
- `pendingBalanceMinor`
- `currency`
- `lastLedgerJournalId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Available and pending balances are shown to users.
- Projection can be rebuilt from ledger entries.
- Projection must never be the financial source of truth.
- Pending withdrawal reduces available balance and increases pending balance until callback completes or fails.
- Instant money movements such as transfer and merchant payment do not use a separate wallet reservation in MVP; they rely on Transaction Service idempotency, authoritative Ledger Service posting, database constraints, and locked/serialized balance validation to prevent double spend.
- Pending money movements such as merchant withdrawal are represented by posted ledger journals that move funds into clearing accounts, not by mutable balance-only holds.

Edge cases:

- Projection lag: API may show `lastUpdatedAt`; critical validations should query authoritative state or use locked transaction logic.
- Negative available balance is never allowed.

## Ledger Service Models

### LedgerAccount

Represents an accounting account.

Fields:

- `id`
- `publicId`
- `ownerType`: `CUSTOMER`, `MERCHANT`, `SYSTEM`
- `ownerId`
- `accountType`: `CUSTOMER_WALLET`, `MERCHANT_BALANCE`, `PAYMENT_CLEARING`, `PAYOUT_CLEARING`, `FEE_REVENUE`, `SUSPENSE`
- `currency`
- `status`: `ACTIVE`, `SUSPENDED`, `CLOSED`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Ledger accounts are not deleted.
- Closed accounts cannot receive ordinary new journals except reversals/corrections.

### LedgerJournal

Represents one balanced accounting journal.

Fields:

- `id`
- `publicId`
- `transactionId`
- `journalType`: `TOP_UP`, `TRANSFER`, `MERCHANT_PAYMENT`, `MERCHANT_WITHDRAWAL`, `REFUND`, `FEE`, `REVERSAL`
- `status`: `POSTED`, `REVERSED`
- `currency`
- `totalDebitMinor`
- `totalCreditMinor`
- `reversalOfJournalId`
- `postedAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- `totalDebitMinor` must equal `totalCreditMinor`.
- Posted journals are immutable.
- Reversal creates a new journal; it does not mutate the original.

Edge cases:

- Duplicate journal command: reject by `transactionId` plus journal type or idempotency key.
- Partial failure after journal post: transaction recovery must detect existing journal and continue idempotently.

### LedgerEntry

Represents one debit or credit line.

Fields:

- `id`
- `ledgerJournalId`
- `ledgerAccountId`
- `direction`: `DEBIT`, `CREDIT`
- `amountMinor`
- `currency`
- `memo`
- `createdAt`
- `updatedAt`

Rules:

- Entry amount must be positive.
- Direction controls accounting meaning; do not store negative entry amounts.
- Entries are immutable after journal is posted.

## Transaction Service Models

### Transaction

Represents lifecycle of a business money movement.

Fields:

- `id`
- `publicReference`
- `type`: `TOP_UP`, `TRANSFER`, `MERCHANT_PAYMENT`, `MERCHANT_WITHDRAWAL`, `REFUND`
- `status`: `INITIATED`, `PENDING_PAYMENT`, `PENDING_PAYOUT`, `PENDING_ADMIN_APPROVAL`, `PROCESSING`, `COMPLETED`, `FAILED`, `EXPIRED`, `REJECTED`, `REFUNDED`, `PARTIALLY_REFUNDED`
- `sourceWalletId`
- `destinationWalletId`
- `merchantId`
- `amountMinor`
- `feeAmountMinor`
- `netAmountMinor`
- `currency`
- `idempotencyKey`
- `fraudDecision`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Every money-moving command requires idempotency key.
- `feeAmountMinor` is flat configurable merchant fee for merchant payments.
- `netAmountMinor = amountMinor - feeAmountMinor` where merchant receives net amount.
- Transaction status is append-audited through events/history.

Edge cases:

- Same idempotency key and same request returns original result.
- Same idempotency key and different request is rejected.
- Admin retry after fraud review reuses the original transaction reference.
- Ledger post must happen at most once per transaction step.

### IdempotencyRecord

Prevents duplicate command execution.

Fields:

- `id`
- `scope`: service or endpoint name.
- `actorId`
- `idempotencyKey`
- `requestHash`
- `responseReference`
- `status`: `IN_PROGRESS`, `COMPLETED`, `FAILED`
- `expiresAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Unique key: `scope + actorId + idempotencyKey`.
- Different request hash with same key is rejected.
- `IN_PROGRESS` records need timeout recovery.

### QrPaymentRequest

Merchant-presented fixed-amount QR request.

Fields:

- `id`
- `publicId`
- `merchantId`
- `amountMinor`
- `currency`
- `orderReference`
- `status`: `ACTIVE`, `PAID`, `EXPIRED`, `CANCELLED`
- `expiresAt`
- `paidTransactionId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Amount is fixed by merchant.
- QR cannot be paid twice.
- QR expiry default is 10 minutes.
- Merchant must be active at generation and payment confirmation.

### TransferQrToken

QR token used for customer-to-customer transfer recipient discovery.

Fields:

- `id`
- `recipientWalletId`
- `tokenHash`
- `status`: `ACTIVE`, `EXPIRED`, `REVOKED`
- `expiresAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Token identifies recipient only; sender enters amount.
- Token should be short-lived.
- Do not embed raw wallet IDs in QR payload without signing or tokenization.

### RefundRequest

Represents merchant-requested refund requiring admin approval.

Fields:

- `id`
- `publicId`
- `originalTransactionId`
- `merchantId`
- `customerWalletId`
- `amountMinor`
- `currency`
- `status`: `REQUESTED`, `PENDING_ADMIN_APPROVAL`, `APPROVED`, `REJECTED`, `PROCESSING`, `COMPLETED`, `FAILED`
- `requestedBy`
- `reviewedBy`
- `reviewReason`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- MVP supports full refunds.
- Admin approval is required before reversal ledger journal.
- Original payment remains immutable.
- Refund transaction links to original payment.

Edge cases:

- Merchant balance insufficient: refund remains failed/rejected and no ledger entries post.
- Original payment already refunded: reject duplicate refund.
- Customer wallet frozen: refund credit is allowed.

## Payment Service Models

### PaymentInstruction

Represents generic simulated top-up payment.

Fields:

- `id`
- `publicId`
- `transactionId`
- `providerReference`
- `amountMinor`
- `currency`
- `status`: `CREATED`, `PENDING`, `AUTHORIZED`, `PAID`, `FAILED`, `EXPIRED`, `CANCELLED`
- `expiresAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Provider is generic in MVP.
- Payment success triggers transaction completion workflow.
- Expired/failed payment must not change wallet balance.

### PayoutInstruction

Represents simulated merchant withdrawal payout.

Fields:

- `id`
- `publicId`
- `transactionId`
- `merchantId`
- `destinationId`
- `amountMinor`
- `currency`
- `status`: `CREATED`, `PENDING_CALLBACK`, `COMPLETED`, `FAILED`
- `providerReference`
- `callbackReceivedAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Withdrawal does not complete synchronously.
- Callback completes or fails payout.
- Duplicate callbacks are idempotent.

## Fraud Service Models

### FraudAlert

Represents risk decision and admin workflow.

Fields:

- `id`
- `publicId`
- `transactionId`
- `subjectType`: `CUSTOMER`, `MERCHANT`, `WALLET`, `TRANSACTION`
- `subjectId`
- `decision`: `ALLOW`, `REVIEW`, `BLOCK`
- `status`: `OPEN`, `RESOLVED`, `RETRIED`, `DISMISSED`
- `ruleCode`
- `riskScore`
- `reason`
- `resolvedBy`
- `resolvedAt`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- `REVIEW` blocks the transaction in MVP.
- Blocked review transaction posts no ledger entries.
- Admin can resolve and retry the original transaction.
- Retry action must be audited.

Edge cases:

- Retry fails another fraud rule: create or update alert; do not loop automatically.
- Admin resolves alert after transaction expiry: retry should fail with expiry/business-state reason.

## Notification Service Models

### Notification

Represents user-facing notification.

Fields:

- `id`
- `recipientType`: `CUSTOMER`, `MERCHANT`, `ADMIN`
- `recipientId`
- `channel`: `IN_APP`, `PUSH`, `EMAIL`
- `type`
- `title`
- `body`
- `status`: `PENDING`, `SENT`, `FAILED`, `READ`
- `relatedEntityType`
- `relatedEntityId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- MVP starts with in-app notification.
- Notification failure must not roll back money movement.

## Audit Models

### AuditLog

Immutable log of important actions.

Fields:

- `id`
- `actor`
- `action`
- `targetType`
- `targetId`
- `beforeStatus`
- `afterStatus`
- `reason`
- `correlationId`
- `metadata`
- `createdAt`
- `updatedAt`

Rules:

- Audit log is append-only.
- `updatedAt` should equal `createdAt` unless storage framework requires update metadata.
- Do not delete audit logs.

## Reporting Models

Reporting models are MongoDB projections derived from Kafka events.

Examples:

- `DailyTransactionSummary`
- `KycReviewSummary`
- `KybReviewSummary`
- `MerchantSettlementSummary`
- `FraudAlertSummary`
- `FeeRevenueSummary`

Common fields:

- `id`
- `projectionDate`
- `currency`
- `metrics`
- `lastEventId`
- `createdAt`
- `updatedAt`
- `version`

Rules:

- Consumers must be idempotent by event ID.
- Projection rebuild must be possible from Kafka/event history where practical.
- Reporting is not the source of truth for money.

## State Transition Edge Cases

### KYC/KYB

- Rejection count increments only on admin rejection, not resubmission request.
- Lock occurs after the third rejection by default.
- Admin unlock resets status to `RESUBMISSION_REQUIRED`, not `APPROVED`.
- Approval after lock is not allowed without unlock.

### Wallet And Balance

- Pending withdrawal reduces available balance immediately.
- Failed payout releases pending balance back to available balance.
- Completed payout clears pending balance.
- Refund credits increase available balance.
- A failed merchant withdrawal must post a compensating reversal journal that debits payout clearing and credits the merchant business balance before the Wallet projection releases pending balance.

### Merchant Payment Fee

- Flat fee must not exceed payment amount.
- If fee equals amount, merchant net amount is zero; decide by config whether this is allowed.
- Fee ledger entry must post in the same journal as merchant payment or in a linked fee journal.

### Refund

- Refund amount cannot exceed original payment amount minus already refunded amount.
- MVP full refund means no previous refund exists and amount equals original payment amount.
- Refund failure after admin approval must leave original payment untouched.

### Admin Retry

- Retry must preserve original business intent.
- Retry must not reuse stale wallet or merchant state; all validations run again.
- Retry must not bypass idempotency or ledger uniqueness checks.

## Implementation Notes

- Prefer enums with explicit database constraints.
- Prefer optimistic locking for mutable workflow records.
- Use database uniqueness constraints for idempotency and duplicate prevention.
- Use append-only history tables for status decisions where audit matters.
- Keep command models separate from read projections.
- Keep Avro event schemas aligned with model lifecycle events, not raw database tables.
