# FinFlow Event Contracts

## Purpose

This document defines FinFlow's event contracts before implementation. It covers Kafka Avro integration events, Schema Registry rules, topic ownership, event envelopes, required event families, consumer responsibilities, and how Kafka events relate to Axon domain events.

Kafka events are for cross-service integration, reporting, notification fan-out, audit streams, and analytics-style consumers. They are not the source of truth for money. Ledger Service remains the accounting source of truth.

## Eventing Principles

1. Kafka integration events must use Avro.
2. Schemas must be registered in Schema Registry.
3. Schema compatibility mode is `BACKWARD`.
4. Services must not publish untyped JSON integration events.
5. Events represent committed facts, not requests.
6. Consumers must be idempotent by `eventId`.
7. Event payloads must not expose secrets, raw identity numbers, raw bank account numbers, or document bytes.
8. Events must include correlation and causation metadata.
9. Event names should be business facts in past tense.
10. Event schemas evolve by adding optional fields with defaults or other backward-compatible changes.

## Axon vs Kafka

Axon:

- Used for command handling, aggregate events, sagas, and workflow coordination.
- May contain service-internal domain events.
- Can model fine-grained state transitions inside a bounded context.

Kafka:

- Used for integration events that other services can consume safely.
- Uses stable Avro contracts.
- Should not expose aggregate internals.
- Should not be required for local transactional correctness.

Rule:

- A service may translate one or more Axon/domain events into one Kafka integration event after local transaction commit.

Example:

```text
Axon/domain events:
  PaymentQrValidated
  MerchantFeeCalculated
  MerchantPaymentJournalRequested

Kafka integration event:
  MerchantPaymentCompleted
```

## Schema Location

Planned repository structure:

```text
contracts/
  avro/
    common/
      EventMetadata.avsc
      Money.avsc
      ActorRef.avsc
    user/
    kyc/
    merchant/
    wallet/
    ledger/
    transaction/
    payment/
    fraud/
    notification/
```

Generated Java classes should be produced from these schemas. Services must depend on generated/schema-bound types rather than hand-built maps.

## Topic Naming

Topic format:

```text
finflow.<domain>.events
```

Initial topics:

| Topic | Owning Service | Purpose |
| --- | --- | --- |
| `finflow.user.events` | User Service | User and customer account lifecycle |
| `finflow.kyc.events` | KYC Service | Customer KYC lifecycle |
| `finflow.merchant.events` | Merchant Service | Merchant, KYB, withdrawal destination lifecycle |
| `finflow.wallet.events` | Wallet Service | Wallet state and balance projection facts |
| `finflow.ledger.events` | Ledger Service | Ledger account, journal, reversal facts |
| `finflow.transaction.events` | Transaction Service | Transaction, QR, refund, withdrawal lifecycle |
| `finflow.payment.events` | Payment Service | Simulated payment and payout facts |
| `finflow.fraud.events` | Fraud Service | Fraud decisions and review lifecycle |
| `finflow.notification.events` | Notification Service | Notification delivery lifecycle |
| `finflow.audit.events` | Owning services | Optional centralized audit stream |

## Partitioning

Partition key rules:

- User/customer events: `userId` or `customerId`.
- KYC events: `kycApplicationId`.
- Merchant/KYB events: `merchantId`.
- Wallet events: `walletId`.
- Ledger events: `ledgerJournalId` for journal facts; `ledgerAccountId` for account facts.
- Transaction events: `transactionId`.
- Payment events: `paymentInstructionId` or `payoutInstructionId`.
- Fraud events: `fraudAlertId` or `transactionId`.
- Notification events: `recipientId`.

Rules:

- Use stable string keys.
- Prefer ordering by aggregate/workflow ID over global ordering.
- Do not rely on ordering across topics.

## Event Metadata

Every event must include metadata fields.

Conceptual Avro shape:

```json
{
  "eventId": "evt_01HT...",
  "eventType": "MerchantPaymentCompleted",
  "eventVersion": 1,
  "occurredAt": "2026-06-08T10:15:30Z",
  "producer": "transaction-service",
  "correlationId": "018f7f3f-2c6c-7d1b-9b77-3e4f1a5e9c10",
  "causationId": "cmd_01HT...",
  "aggregateType": "Transaction",
  "aggregateId": "txn_01HT...",
  "actorId": "usr_01HT...",
  "actorType": "CUSTOMER"
}
```

Required metadata:

- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `producer`
- `correlationId`
- `causationId`
- `aggregateType`
- `aggregateId`

Optional metadata:

- `actorId`
- `actorType`
- `tenantId` if multi-tenancy is added later.

## Common Value Schemas

### Money

Fields:

- `amountMinor`: long
- `currency`: string

Rules:

- MVP currency is `IDR`.
- No floating-point amounts.

### ActorRef

Fields:

- `actorId`: nullable string
- `actorType`: nullable enum: `CUSTOMER`, `MERCHANT_OWNER`, `ADMIN`, `COMPLIANCE`, `SUPPORT`, `SYSTEM`, `SERVICE`
- `role`: nullable string

### DocumentRef

Fields:

- `documentId`: string
- `documentType`: string
- `contentType`: nullable string
- `sizeBytes`: nullable long

Rules:

- Do not put MinIO/S3 signed URLs in Kafka events.
- Do not put raw document bytes in Kafka events.

## Event Families

The tables below define the initial integration events. Exact Avro field names may be refined during schema generation, but ownership, meaning, and required facts should remain stable.

## User Events

Topic:

```text
finflow.user.events
```

### UserProfileCreated

Producer:

- User Service

Key:

- `userId`

Payload fields:

- `userId`
- `keycloakSubjectId`
- `email`
- `phoneNumber`
- `displayName`
- `status`
- `createdAt`

Consumers:

- KYC Service
- Merchant Service
- Reporting Service
- Notification Service

### CustomerAccountCreated

Payload fields:

- `customerId`
- `userId`
- `status`
- `createdAt`

### CustomerAccountSuspended

Payload fields:

- `customerId`
- `userId`
- `previousStatus`
- `newStatus`
- `reason`
- `changedBy`

### CustomerAccountClosed

Payload fields:

- `customerId`
- `userId`
- `reason`
- `closedBy`
- `closedAt`

## KYC Events

Topic:

```text
finflow.kyc.events
```

Producer:

- KYC Service

### KycSubmitted

Key:

- `kycApplicationId`

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `status`
- `submittedAt`
- `documentTypes`

### KycApproved

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `approvedBy`
- `approvedAt`
- `rejectionCount`

Consumers:

- User Service
- Wallet Service
- Merchant Service
- Reporting Service
- Notification Service

### KycRejected

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `rejectedBy`
- `rejectedAt`
- `reason`
- `rejectionCount`
- `remainingAttempts`

### KycResubmissionRequired

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `requestedBy`
- `requestedAt`
- `reason`

### KycLocked

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `lockedAt`
- `rejectionCount`
- `reason`

### KycUnlocked

Payload fields:

- `kycApplicationId`
- `userId`
- `customerId`
- `unlockedBy`
- `unlockedAt`
- `reason`

## Merchant Events

Topic:

```text
finflow.merchant.events
```

Producer:

- Merchant Service

### MerchantRegistered

Key:

- `merchantId`

Payload fields:

- `merchantId`
- `ownerUserId`
- `merchantName`
- `businessCategory`
- `businessType`
- `status`
- `registeredAt`

### KybSubmitted

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `businessName`
- `businessCategory`
- `businessType`
- `submittedAt`
- `documentTypes`

### KybApproved

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `approvedBy`
- `approvedAt`
- `rejectionCount`

Consumers:

- Wallet Service
- Transaction Service
- Reporting Service
- Notification Service

### KybRejected

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `rejectedBy`
- `rejectedAt`
- `reason`
- `rejectionCount`
- `remainingAttempts`

### KybResubmissionRequired

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `requestedBy`
- `requestedAt`
- `reason`

### KybLocked

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `lockedAt`
- `rejectionCount`
- `reason`

### KybUnlocked

Payload fields:

- `kybApplicationId`
- `merchantId`
- `ownerUserId`
- `unlockedBy`
- `unlockedAt`
- `reason`

### MerchantActivated

Payload fields:

- `merchantId`
- `ownerUserId`
- `businessWalletId`
- `activatedAt`

### MerchantSuspended

Payload fields:

- `merchantId`
- `previousStatus`
- `newStatus`
- `reason`
- `suspendedBy`
- `suspendedAt`

### WithdrawalDestinationVerified

Payload fields:

- `withdrawalDestinationId`
- `merchantId`
- `destinationType`
- `verifiedAt`

## Wallet Events

Topic:

```text
finflow.wallet.events
```

Producer:

- Wallet Service

### WalletCreated

Key:

- `walletId`

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `ledgerAccountId`
- `currency`
- `status`
- `createdAt`

### MerchantWalletCreated

Payload fields:

- `walletId`
- `merchantId`
- `ownerUserId`
- `ledgerAccountId`
- `currency`
- `status`
- `createdAt`

### WalletActivated

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `activatedAt`

### WalletFrozen

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `reason`
- `frozenBy`
- `frozenAt`

### WalletUnfrozen

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `reason`
- `unfrozenBy`
- `unfrozenAt`

### WalletClosed

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `reason`
- `closedBy`
- `closedAt`

### BalanceProjectionUpdated

Payload fields:

- `walletId`
- `ownerType`
- `ownerId`
- `availableBalance`
- `pendingBalance`
- `currency`
- `lastLedgerJournalId`
- `updatedAt`

Rules:

- This event is a projection update, not accounting truth.
- Consumers must not use it as the financial source of truth.

## Ledger Events

Topic:

```text
finflow.ledger.events
```

Producer:

- Ledger Service

### LedgerAccountCreated

Key:

- `ledgerAccountId`

Payload fields:

- `ledgerAccountId`
- `ownerType`
- `ownerId`
- `accountType`
- `currency`
- `createdAt`

### LedgerJournalPosted

Key:

- `ledgerJournalId`

Payload fields:

- `ledgerJournalId`
- `transactionId`
- `publicReference`
- `journalType`
- `currency`
- `totalDebitMinor`
- `totalCreditMinor`
- `postedAt`
- `entryCount`

Rules:

- Journal details may be queried through Ledger API by authorized admin/compliance users.
- Events should not include full sensitive memo fields unless required by consumers.

Consumers:

- Wallet Service
- Transaction Service
- Reporting Service

### LedgerJournalRejected

Payload fields:

- `transactionId`
- `journalType`
- `reasonCode`
- `reason`
- `rejectedAt`

### LedgerJournalReversed

Payload fields:

- `ledgerJournalId`
- `reversalJournalId`
- `transactionId`
- `reason`
- `reversedAt`

## Transaction Events

Topic:

```text
finflow.transaction.events
```

Producer:

- Transaction Service

### TransactionInitiated

Key:

- `transactionId`

Payload fields:

- `transactionId`
- `publicReference`
- `type`
- `actorId`
- `sourceWalletId`
- `destinationWalletId`
- `merchantId`
- `amount`
- `status`
- `initiatedAt`

### TopUpRequested

Payload fields:

- `transactionId`
- `customerId`
- `walletId`
- `amount`
- `requestedAt`

Consumers:

- Payment Service
- Fraud Service
- Reporting Service

### TransferRequested

Payload fields:

- `transactionId`
- `senderWalletId`
- `recipientWalletId`
- `amount`
- `note`
- `requestedAt`

### MerchantPaymentQrCreated

Payload fields:

- `qrPaymentRequestId`
- `merchantId`
- `amount`
- `orderReference`
- `expiresAt`
- `createdAt`

### MerchantPaymentRequested

Payload fields:

- `transactionId`
- `qrPaymentRequestId`
- `customerWalletId`
- `merchantId`
- `amount`
- `flatFee`
- `merchantNetAmount`
- `requestedAt`

Consumers:

- Fraud Service
- Ledger Service through workflow/command bridge if applicable.
- Reporting Service

### MerchantPaymentCompleted

Payload fields:

- `transactionId`
- `publicReference`
- `customerWalletId`
- `merchantId`
- `merchantWalletId`
- `amount`
- `flatFee`
- `merchantNetAmount`
- `ledgerJournalId`
- `completedAt`

Consumers:

- Merchant Service
- Wallet Service
- Notification Service
- Reporting Service

### MerchantWithdrawalRequested

Payload fields:

- `transactionId`
- `merchantId`
- `merchantWalletId`
- `withdrawalDestinationId`
- `amount`
- `requestedAt`

Consumers:

- Payment Service
- Fraud Service
- Reporting Service

### MerchantWithdrawalPending

Payload fields:

- `transactionId`
- `merchantId`
- `merchantWalletId`
- `amount`
- `payoutInstructionId`
- `ledgerJournalId`
- `pendingAt`

### MerchantWithdrawalCompleted

Payload fields:

- `transactionId`
- `merchantId`
- `merchantWalletId`
- `amount`
- `payoutInstructionId`
- `completedAt`

### MerchantWithdrawalFailed

Payload fields:

- `transactionId`
- `merchantId`
- `merchantWalletId`
- `amount`
- `payoutInstructionId`
- `reason`
- `failedAt`

### RefundRequested

Payload fields:

- `refundRequestId`
- `originalTransactionId`
- `merchantId`
- `customerWalletId`
- `amount`
- `requestedBy`
- `requestedAt`
- `reason`

Consumers:

- Fraud Service
- Notification Service
- Reporting Service

### RefundApproved

Payload fields:

- `refundRequestId`
- `originalTransactionId`
- `approvedBy`
- `approvedAt`
- `reason`

### RefundRejected

Payload fields:

- `refundRequestId`
- `originalTransactionId`
- `rejectedBy`
- `rejectedAt`
- `reason`

### RefundCompleted

Payload fields:

- `refundRequestId`
- `refundTransactionId`
- `originalTransactionId`
- `merchantId`
- `customerWalletId`
- `amount`
- `ledgerJournalId`
- `completedAt`

### TransactionCompleted

Payload fields:

- `transactionId`
- `publicReference`
- `type`
- `amount`
- `feeAmount`
- `netAmount`
- `completedAt`

### TransactionFailed

Payload fields:

- `transactionId`
- `publicReference`
- `type`
- `reasonCode`
- `reason`
- `failedAt`

### TransactionRejected

Payload fields:

- `transactionId`
- `publicReference`
- `type`
- `reasonCode`
- `reason`
- `rejectedAt`

## Payment Events

Topic:

```text
finflow.payment.events
```

Producer:

- Payment Service

### PaymentInstructionCreated

Payload fields:

- `paymentInstructionId`
- `transactionId`
- `amount`
- `providerReference`
- `expiresAt`
- `createdAt`

### PaymentPaid

Payload fields:

- `paymentInstructionId`
- `transactionId`
- `amount`
- `providerReference`
- `paidAt`

### PaymentFailed

Payload fields:

- `paymentInstructionId`
- `transactionId`
- `reasonCode`
- `reason`
- `failedAt`

### PaymentExpired

Payload fields:

- `paymentInstructionId`
- `transactionId`
- `expiredAt`

### PayoutInstructionCreated

Payload fields:

- `payoutInstructionId`
- `transactionId`
- `merchantId`
- `withdrawalDestinationId`
- `amount`
- `providerReference`
- `createdAt`

### PayoutCompleted

Payload fields:

- `payoutInstructionId`
- `transactionId`
- `merchantId`
- `amount`
- `providerReference`
- `completedAt`

### PayoutFailed

Payload fields:

- `payoutInstructionId`
- `transactionId`
- `merchantId`
- `amount`
- `providerReference`
- `reasonCode`
- `reason`
- `failedAt`

Rules:

- Duplicate payout callbacks must not produce duplicate `PayoutCompleted` events.

## Fraud Events

Topic:

```text
finflow.fraud.events
```

Producer:

- Fraud Service

### FraudAllowed

Payload fields:

- `transactionId`
- `fraudAlertId`
- `decision`
- `ruleCode`
- `riskScore`
- `decidedAt`

### FraudReviewRequired

Payload fields:

- `transactionId`
- `fraudAlertId`
- `subjectType`
- `subjectId`
- `decision`
- `ruleCode`
- `riskScore`
- `reason`
- `createdAt`

Rules:

- `REVIEW` blocks the transaction in MVP.
- Transaction Service must not post ledger entries for a review-blocked transaction.

### FraudBlocked

Payload fields:

- `transactionId`
- `fraudAlertId`
- `subjectType`
- `subjectId`
- `ruleCode`
- `riskScore`
- `reason`
- `blockedAt`

### FraudAlertResolved

Payload fields:

- `fraudAlertId`
- `transactionId`
- `resolution`
- `resolvedBy`
- `resolvedAt`
- `reason`

### FraudReviewRetryRequested

Payload fields:

- `fraudAlertId`
- `transactionId`
- `requestedBy`
- `requestedAt`
- `reason`

Consumers:

- Transaction Service
- Audit/reporting consumers

Rules:

- Retry does not bypass current transaction validation.
- Retry must be audited by Fraud Service and Transaction Service.

## Notification Events

Topic:

```text
finflow.notification.events
```

Producer:

- Notification Service

### NotificationCreated

Payload fields:

- `notificationId`
- `recipientType`
- `recipientId`
- `channel`
- `type`
- `relatedEntityType`
- `relatedEntityId`
- `createdAt`

### NotificationSent

Payload fields:

- `notificationId`
- `recipientType`
- `recipientId`
- `channel`
- `sentAt`

### NotificationFailed

Payload fields:

- `notificationId`
- `recipientType`
- `recipientId`
- `channel`
- `reasonCode`
- `reason`
- `failedAt`

### NotificationRead

Payload fields:

- `notificationId`
- `recipientType`
- `recipientId`
- `readAt`

Rules:

- Notification failure never rolls back the business event that caused it.

## Audit Events

Topic:

```text
finflow.audit.events
```

Producer:

- Any service that owns the audited action.

### AuditActionRecorded

Payload fields:

- `auditLogId`
- `serviceName`
- `actorId`
- `actorType`
- `role`
- `action`
- `targetType`
- `targetId`
- `beforeStatus`
- `afterStatus`
- `reason`
- `recordedAt`

Rules:

- Local service audit tables remain the source of truth.
- Audit Kafka events are for centralized reporting/search.

## Event Versioning

Rules:

- Start every schema at version `1`.
- Additive optional fields with defaults are allowed.
- Removing fields is not allowed without a new event type.
- Renaming fields is treated as remove plus add and should be avoided.
- Changing field type is not allowed unless Schema Registry confirms backward compatibility.
- Enum additions must be reviewed because some consumers may not handle unknown values.

Schema Registry:

- Compatibility: `BACKWARD`.
- CI must check schema compatibility before merge.
- Services must fail startup if required schemas cannot be registered or loaded in non-test environments.

## Outbox Pattern

Services that publish Kafka events after database state changes should use an outbox pattern.

Outbox fields:

- `id`
- `aggregateType`
- `aggregateId`
- `eventType`
- `eventVersion`
- `payload`
- `headers`
- `status`: `PENDING`, `PUBLISHED`, `FAILED`
- `attemptCount`
- `nextAttemptAt`
- `createdAt`
- `updatedAt`

Rules:

- Write business state and outbox row in the same database transaction.
- Publish asynchronously from outbox.
- Mark outbox row as published only after broker acknowledgement.
- Publisher retries must be safe.

## Consumer Idempotency

Every consumer must track processed event IDs or otherwise guarantee idempotency.

Recommended consumer offset table:

- `id`
- `consumerName`
- `eventId`
- `topic`
- `partition`
- `offset`
- `processedAt`

Rules:

- Duplicate events must not create duplicate notifications, reports, balance updates, or audit records.
- Consumers must tolerate out-of-order events where topic/partition ordering does not protect them.
- Consumers should ignore events with unknown future fields.

## Privacy And Security

Events must not include:

- Raw national identity numbers.
- Raw bank account numbers.
- Passwords or tokens.
- MinIO/S3 signed URLs.
- Document bytes.
- Sensitive admin-only notes unless needed by authorized consumers.

Events may include:

- Public IDs.
- Hashed identifiers when needed for correlation.
- Masked destination/account values.
- Business statuses and timestamps.

## Testing Requirements

Schema tests:

- Avro schemas compile.
- Schema Registry compatibility check passes in `BACKWARD` mode.
- Generated Java classes compile.

Producer tests:

- Producer serializes with registered schema.
- Required metadata is present.
- Sensitive fields are not present.
- Outbox record is created in the same transaction as state change.

Consumer tests:

- Consumer handles duplicate event ID idempotently.
- Consumer handles unknown optional fields.
- Consumer rejects or dead-letters incompatible messages.
- Consumer does not treat projection data as source of truth.

Workflow tests:

- KYC approval eventually activates wallet.
- KYB approval eventually activates merchant wallet.
- Merchant payment emits fee and ledger events.
- Withdrawal emits pending and callback-completion events.
- Refund emits requested, approved/rejected, and completed events.
- Fraud review emits blocking event and retry request event.

## Dead Letter Handling

Consumers should use a dead-letter topic or parking mechanism for messages that cannot be processed after retries.

Recommended naming:

```text
finflow.<domain>.events.dlq
```

Rules:

- Transient errors retry with backoff.
- Permanent schema/validation errors go to DLQ.
- DLQ messages include original topic, partition, offset, event ID, error code, and error message.
- DLQ reprocessing must be manual or explicitly controlled.

## Open Event Questions

These can be decided during schema generation:

1. Should audit events be published by every service in MVP, or added after core workflows work?
2. Should ledger events include entry-level details, or should consumers query Ledger API for details?
3. Should event type use one topic per domain as documented, or one topic per event family for higher isolation?
4. Should generated Avro classes live in one shared package or per-domain contract modules?
