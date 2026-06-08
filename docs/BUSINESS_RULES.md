# FinFlow Business Rules

## Purpose

This document defines business rules that should be enforced by services, tests, and admin workflows. These rules are intentionally explicit so implementation does not drift into unclear wallet behavior.

## Global Rules

1. Only approved customers can perform wallet transactions.
2. Only approved merchants can receive merchant payments.
3. Every money movement must produce balanced ledger entries.
4. Ledger entries are immutable.
5. Corrections use reversal entries.
6. Every money-moving request must include an idempotency key.
7. Every admin decision must be audited.
8. Kafka integration events must use Avro schemas and Schema Registry.
9. Payment provider simulation must stay generic and provider-neutral.
10. Customer and merchant wallet views show available balance and pending balance.
11. Fraud `REVIEW` blocks the transaction and requires admin resolution.
12. FinFlow is not a licensed real-money system; production launch would require legal, compliance, risk, and licensing review.

## Roles And Permissions

### Customer

Can:

- Submit KYC.
- View own profile.
- View own wallet.
- Top up after approval.
- Transfer after approval.
- Pay merchants after approval.
- View own transaction history.

Cannot:

- Transact before KYC approval.
- Access other users' transactions.
- Override fraud decisions.
- Change ledger records.

### Merchant

Can:

- Submit KYB after owner identity is linked.
- View own merchant profile.
- Generate QR payment requests after approval.
- View own incoming payments.
- Request withdrawal after approval.

Cannot:

- Receive payment before KYB approval.
- View other merchants' transactions.
- Modify payment records.
- Bypass withdrawal limits.

### Admin

Can:

- Review KYC and KYB.
- Approve, reject, or request resubmission.
- View customer and merchant status.
- Freeze or unfreeze wallets and merchants.
- Approve or reject refund requests.
- Retry a fraud-review-blocked transaction after resolving the alert.
- View transactions and reports.

Cannot:

- Mutate ledger entries.
- Create money movement without a business transaction.
- Delete audit logs.

## KYC Rules

Required customer data:

- Legal name.
- Date of birth.
- National identity number.
- Phone number.
- Address.
- Identity document image.
- Selfie or liveness demo image.

Rules:

- KYC status starts as `NOT_SUBMITTED`.
- Customer can submit KYC once required fields are complete.
- Submitting KYC changes status to `PENDING_REVIEW`.
- Admin approval changes status to `APPROVED`.
- Admin rejection changes status to `REJECTED`.
- Admin can request resubmission with a reason.
- Customer cannot transact unless KYC status is `APPROVED`.
- KYC documents are stored in MinIO locally, not in PostgreSQL blobs.
- KYC decision history is append-only.

## KYB Rules

Required merchant data:

- Business name.
- Business category.
- Business type.
- Business address.
- Owner user ID.
- Owner approved KYC status.
- Business document metadata.
- Withdrawal destination.

Optional but recommended data:

- Business registration number.
- Tax number.
- Storefront photo or proof of operation.

Rules:

- Merchant owner must be an approved customer before merchant KYB can be approved.
- KYB status starts as `NOT_SUBMITTED`.
- Submitting KYB changes status to `PENDING_REVIEW`.
- Admin approval changes status to `APPROVED`.
- Merchant activation can happen only after KYB approval.
- Admin rejection changes status to `REJECTED`.
- Admin can request resubmission with a reason.
- Merchant cannot receive payments unless status is `APPROVED` and merchant account is `ACTIVE`.
- KYB decision history is append-only.

## Wallet Rules

Wallet states:

- `PENDING_ACTIVATION`
- `ACTIVE`
- `FROZEN`
- `CLOSED`

Rules:

- Wallet is created after customer KYC approval.
- Wallet balance is a projection, not the financial source of truth.
- Ledger is the source of truth.
- Wallet cannot go below zero.
- Frozen wallet cannot send money, pay merchants, or withdraw.
- Frozen wallet can still receive reversal/refund entries.
- Closed wallet cannot initiate or receive ordinary transactions.
- Balance projections must expose available balance and pending balance.

## Ledger Rules

Ledger account types:

- Customer wallet account.
- Merchant business balance account.
- External payment clearing account.
- External payout clearing account.
- Fee revenue account.
- Suspense account for later manual review.

Rules:

- Every journal must balance.
- Total debits must equal total credits.
- Ledger entries are append-only.
- Journal status must be traceable to a business transaction.
- Reversals must reference the original journal.
- Ledger service must reject unbalanced journals.
- Ledger service must reject duplicate journal IDs.

## Transaction Rules

Transaction statuses:

```text
INITIATED
  -> PENDING_PAYMENT
  -> PROCESSING
  -> COMPLETED

INITIATED
  -> REJECTED

PENDING_PAYMENT
  -> EXPIRED
  -> FAILED

PROCESSING
  -> FAILED
  -> COMPLETED

COMPLETED
  -> REFUNDED
  -> PARTIALLY_REFUNDED
```

Rules:

- A transaction must have exactly one type.
- Supported MVP types: `TOP_UP`, `TRANSFER`, `MERCHANT_PAYMENT`, `MERCHANT_WITHDRAWAL`, `REFUND`.
- Every transaction must have a stable public reference.
- Money-moving commands require idempotency keys.
- Reusing the same idempotency key with the same request returns the original result.
- Reusing the same idempotency key with a different request is rejected.
- Completed transactions are not deleted.

## Top-Up Rules

Rules:

- Customer must be KYC approved.
- Wallet must be active.
- Amount must be positive.
- Amount must be within configured limits.
- Simulated payment success posts ledger entries.
- Simulated payment failure does not change wallet balance.

## Transfer Rules

Rules:

- Sender must be KYC approved.
- Recipient must be KYC approved.
- Sender and recipient wallets must be active.
- Sender must have sufficient balance.
- Sender and recipient cannot be the same wallet.
- Transfer creates one transaction visible to both parties.
- Ledger journal debits sender and credits recipient.
- MVP recipient discovery uses transfer QR. Phone number and username lookup are not included in the first implementation.

## Merchant Payment Rules

Rules:

- Merchant must be KYB approved and active.
- Customer must be KYC approved and active.
- QR payment request must not be expired.
- QR payment request must not already be paid.
- QR payment amount is fixed by the merchant when the QR request is created.
- Customer must have sufficient balance.
- Ledger journal debits customer wallet and credits merchant business balance.
- Configurable merchant payment fee is posted as part of the ledger journal.
- Merchant sees payment immediately in transaction history after completion.

## Merchant Withdrawal Rules

Rules:

- Merchant must be KYB approved and active.
- Withdrawal destination must be verified.
- Merchant business balance must be sufficient.
- Amount must be positive.
- Amount must be within configured limits.
- Ledger journal debits merchant business balance and credits payout clearing.
- Withdrawal enters pending state first.
- Payment Service completes withdrawal through a simulated payout callback.
- Successful payout clears pending balance.
- Failed payout must post a reversal journal that debits payout clearing and credits merchant business balance before pending balance is released.

## Refund Rules

Rules:

- Refunds are included in MVP.
- Refund can be requested only for a completed merchant payment.
- Refund requires admin approval before ledger reversal is posted.
- MVP supports full refund first.
- Refund must reference the original merchant payment.
- Refund uses reversal ledger entries.
- Original payment transaction remains immutable.
- Refund is blocked if merchant business balance is insufficient.
- Refund is blocked if fraud rules require blocking.
- Customer wallet can receive a refund even if ordinary outgoing transactions are frozen.

## Limits

Initial MVP limits should be configurable, not hardcoded.

Recommended defaults:

- Minimum transaction amount: IDR 1,000.
- Maximum single transfer: IDR 2,000,000.
- Maximum single merchant payment: IDR 5,000,000.
- Maximum daily customer outgoing amount: IDR 10,000,000.
- Maximum daily merchant withdrawal amount: IDR 10,000,000.
- QR payment request expiry: 10 minutes.

These are product defaults for the prototype, not regulatory advice.

## Fees

MVP default:

- Customer top-up fee: IDR 0.
- Customer transfer fee: IDR 0.
- Merchant payment fee: configurable.
- Merchant withdrawal fee: IDR 0.

Merchant payment fee rules:

- Fee is configured as a flat amount.
- Fee is charged to the merchant side in MVP.
- Fee posts to a fee revenue ledger account.
- Show fee breakdown before confirmation.

## Fraud Rules

MVP fraud rules:

- Reject transaction when wallet is frozen.
- Flag customer if outgoing volume exceeds daily threshold.
- Flag merchant if incoming payment count spikes within a short window.
- Flag repeated failed payments.
- Flag withdrawal soon after suspicious incoming volume.

Outcomes:

- `ALLOW`
- `REVIEW`
- `BLOCK`

Rules:

- `REVIEW` blocks the transaction in MVP.
- A blocked review transaction must not post ledger entries.
- Admin can resolve the alert and retry the blocked transaction.
- Retried transactions must reuse the original transaction reference and record the admin retry action in audit logs.
- `HOLD` can be added later for manual review before completion.

## Audit Rules

Audit events must be created for:

- KYC submitted.
- KYC approved/rejected/resubmission requested.
- KYB submitted.
- KYB approved/rejected/resubmission requested.
- Wallet frozen/unfrozen.
- Merchant suspended/reactivated.
- Transaction rejected by fraud.
- Refund requested/completed/failed.
- Refund approved/rejected.
- Fraud transaction retried by admin.
- Admin login and sensitive admin actions.

Audit records must include:

- Actor ID.
- Actor role.
- Action.
- Target entity type.
- Target entity ID.
- Timestamp.
- Correlation ID.
- Reason where applicable.

## Reporting Rules

Admin reports should read from projections, not transactional service databases directly.

Reporting projections should be built from Kafka events:

- KYC events.
- KYB events.
- Transaction events.
- Ledger events.
- Fraud events.

Reporting consumers must be idempotent.

## Rejection Lock Rules

Rules:

- KYC and KYB resubmission is allowed after rejection until a configured rejection limit is reached.
- Default rejection limit is 3.
- After the limit is reached, status becomes `LOCKED`.
- Locked KYC/KYB cannot be resubmitted by the user.
- Admin or compliance reviewer can unlock with an audited reason.

## Answered Business Decisions

1. Balances show available and pending balance.
2. Merchant withdrawals go pending first, then complete through simulated payout callback.
3. Merchant QR amount is fixed by merchant.
4. Customer transfer uses QR in MVP.
5. Merchant payment includes configurable flat fee accounting.
6. Rejected KYC/KYB locks after repeated rejection.
7. Fraud `REVIEW` blocks the transaction.
8. Refunds are included in MVP.
9. Merchant payment fee is a configurable flat fee.
10. Refunds require admin approval.
11. Rejection lock limit defaults to 3.
12. Admin can retry a fraud-review-blocked transaction.
