# FinFlow Business Flows

## Purpose

This document defines how FinFlow should behave as a wallet and merchant-payment product before implementation starts. The goal is to make the product flow concrete enough that backend services, mobile screens, admin screens, events, and tests can be derived from it.

FinFlow is inspired by Indonesian e-wallet and QRIS-style merchant payment patterns, but it is a portfolio/product prototype and is not a licensed financial product.

## Reference Patterns

Public product and regulatory references that shape these flows:

- DANA Premium upgrade asks users to verify identity with an e-KTP/KTP capture flow in the app.
- DANA Bisnis positions merchant QRIS as a way for UMKM merchants to accept digital wallet payments, track incoming transactions, and withdraw business balance.
- DANA's QRIS help states merchants use standardized QRIS, customers scan QRIS and pay, and merchants check successful payments in transaction history.
- Bank Indonesia describes QRIS as a national QR payment standard intended to make one QR code usable across payment applications.
- Bank Indonesia's sectoral risk assessment describes e-KYC, customer identification, transaction restrictions for unregistered e-money, suspicious transaction obligations, and higher risk around offline merchants and cash top-ups.

Sources are listed at the end of this document.

## Business Actors

### Customer

An individual wallet user who can:

- Register and sign in.
- Complete KYC.
- Hold a wallet balance after approval.
- Top up.
- Transfer to another approved customer.
- Pay an approved merchant.
- View transaction history.
- Receive notifications.
- Request account freeze or contact support.

### Merchant Owner

An individual who owns or operates a business and can:

- Register and sign in.
- Submit business KYB data.
- Link personal identity to the business.
- Receive admin approval.
- Generate merchant QR payment requests.
- Receive payments into business balance.
- View business transaction history.
- Request withdrawal from business balance.

### Admin Operator

An internal operations user who can:

- Review KYC applications.
- Review merchant KYB applications.
- Approve or reject onboarding.
- Monitor transactions.
- Review fraud alerts.
- Freeze or unfreeze accounts.
- Inspect audit logs.
- View operational reports.

### Compliance Reviewer

A stricter admin role used for:

- Enhanced due diligence.
- Suspicious activity review.
- Manual risk decisions.
- Exportable audit evidence.

Compliance has distinct permissions for sensitive unlock, fraud, and evidence review actions.

## Account Lifecycle

### Customer Account States

Customer account status is owned by User Service and represents whether the customer account can use wallet features. KYC application status is owned by KYC Service and is tracked separately.

```text
REGISTERED
  -> KYC_IN_REVIEW
  -> KYC_APPROVED
  -> WALLET_ACTIVE
  -> FROZEN
  -> CLOSED

KYC_IN_REVIEW
  -> KYC_REJECTED
  -> KYC_RESUBMISSION_REQUIRED
  -> KYC_LOCKED
```

Rules:

- `REGISTERED` customers can sign in and submit KYC, but cannot transact.
- `KYC_IN_REVIEW` mirrors that KYC Service has an application in `PENDING_REVIEW`.
- `KYC_APPROVED` customers are eligible for wallet activation.
- KYC approval synchronizes the User Service customer account to `KYC_APPROVED`; wallet activation remains a separate Wallet Service workflow.
- `WALLET_ACTIVE` customers can top up, transfer, and pay merchants.
- `FROZEN` customers can sign in and view history but cannot initiate money movement.
- `CLOSED` customers cannot transact and should only retain audit/history access where required.

KYC application statuses are:

```text
NOT_SUBMITTED
  -> PENDING_REVIEW
  -> APPROVED
  -> REJECTED
  -> RESUBMISSION_REQUIRED
  -> LOCKED
```

### Merchant Account States

Merchant account status is owned by Merchant Service and represents the business account lifecycle. KYB application status is a separate review workflow owned by Merchant Service.

```text
REGISTERED
  -> KYB_IN_REVIEW
  -> KYB_APPROVED
  -> ACTIVE
  -> SUSPENDED
  -> CLOSED

KYB_IN_REVIEW
  -> KYB_REJECTED
  -> KYB_RESUBMISSION_REQUIRED
  -> KYB_LOCKED
```

Rules:

- Merchants cannot receive payments before KYB approval.
- A merchant owner must have approved personal KYC before business KYB can be submitted or approved.
- `ACTIVE` merchants can generate QR payment requests and receive payments.
- `SUSPENDED` merchants cannot receive new payments or withdraw.

KYB application statuses are:

```text
NOT_SUBMITTED
  -> PENDING_REVIEW
  -> APPROVED
  -> REJECTED
  -> RESUBMISSION_REQUIRED
  -> LOCKED
```

## Customer Onboarding Flow

1. Customer opens the app.
2. Customer registers or signs in through Keycloak.
3. User Service creates an application profile linked to the Keycloak subject.
4. Customer creates a KYC draft with:
   - Legal name.
   - Date of birth.
   - National identity number.
   - Phone number.
   - Address.
5. Customer uploads required KYC evidence:
   - Identity document image.
   - Selfie or liveness demo image for MVP.
6. KYC Service verifies the uploaded objects exist in object storage.
7. Customer submits the application for admin review.
8. KYC Service stores structured data and document metadata in PostgreSQL.
9. Document files are stored in MinIO locally.
10. Admin reviews KYC evidence in the dashboard.
11. Admin approves, rejects, or requests resubmission.
12. On approval, the customer lifecycle becomes `KYC_APPROVED`.
13. A later Wallet Service slice creates the customer wallet and ledger account before transactions are allowed.

## Merchant Onboarding Flow

1. Merchant owner signs in.
2. Merchant owner must complete personal KYC first.
3. Merchant owner starts KYB onboarding.
4. Merchant submits:
   - Business name.
   - Business category.
   - Business type: individual merchant, CV, PT, cooperative, or other.
   - Business registration number if applicable.
   - Tax number if applicable.
   - Business address.
   - Storefront or business proof document.
   - Bank account or wallet destination for withdrawals.
   - Owner identity linkage.
5. Merchant Service creates or updates the merchant profile to `KYB_IN_REVIEW` and the KYB application to `PENDING_REVIEW`.
6. Merchant Service records KYB review data and document metadata. KYC Service remains responsible only for the owner personal KYC status.
7. Admin reviews KYB documents and business details.
8. Admin approves, rejects, or requests resubmission.
9. On approval:
   - Merchant becomes `ACTIVE`.
   - Merchant business balance account is created.
   - Merchant can generate QR payment requests.

## Top-Up Flow

1. Customer selects top-up amount.
2. App sends top-up command with idempotency key.
3. Transaction Service validates:
   - Customer is KYC approved.
   - Wallet is active.
   - Amount is within limits.
   - Idempotency key has not already created a different result.
4. Payment Service creates generic simulated payment instruction.
5. Customer confirms simulated payment.
6. Payment Service emits payment result.
7. Transaction Service marks top-up as successful.
8. Ledger Service posts balanced journal:
   - Debit external payment clearing account.
   - Credit customer wallet account.
9. Wallet balance projection updates.
10. Notification is created.
11. Transaction appears in history.

Failure cases:

- Payment expires.
- Payment fails.
- Duplicate request with same idempotency key returns the original result.
- Customer is not approved or wallet is frozen.

## Customer Transfer Flow

1. Recipient generates a transfer QR from their wallet.
2. Sender scans the recipient transfer QR.
3. Sender enters amount and note.
4. App sends transfer command with idempotency key.
5. Transaction Service validates:
   - Sender KYC is approved.
   - Sender wallet is active.
   - Recipient exists.
   - Recipient KYC is approved.
   - Recipient wallet is active.
   - Sender has sufficient available balance according to the authoritative ledger posting check.
   - Amount is within limits.
   - Fraud rules do not require blocking.
6. Transaction Service requests an authoritative ledger post after wallet, limit, idempotency, and fraud checks pass.
7. Ledger Service posts the balanced journal exactly once for the transaction step after serializing affected ledger accounts and confirming sufficient posted balance:
   - Debit sender wallet account.
   - Credit recipient wallet account.
8. Transaction Service marks transfer as completed.
9. Sender and recipient receive notifications.
10. Both users see the transaction in history.

Failure cases:

- Insufficient funds.
- Recipient not found.
- Sender or recipient not approved.
- Wallet frozen.
- Fraud rule blocks transaction.

## Merchant QR Payment Flow

MVP uses merchant-presented QR, similar in concept to QRIS merchant-presented mode.

1. Merchant enters fixed amount and optional order reference.
2. Merchant app generates a payment request.
3. System returns QR payload.
4. Customer scans QR.
5. Customer app displays merchant name, amount, and expiry.
6. Customer confirms payment.
7. Transaction Service validates:
   - Customer KYC is approved.
   - Customer wallet is active.
   - Merchant KYB is approved.
   - Merchant account is active.
   - QR payment request is valid and not expired.
   - Customer has sufficient balance according to the authoritative ledger posting check.
8. Ledger Service posts balanced journal:
   - Debit customer wallet account.
   - Credit merchant business balance account net of fee.
   - Credit fee revenue account for configured merchant fee.
9. Transaction Service marks payment as completed.
10. Merchant sees incoming payment in transaction history.
11. Customer receives receipt.
12. Merchant receives payment notification.

Failure cases:

- QR expired.
- QR already paid.
- Merchant suspended.
- Customer wallet frozen.
- Insufficient funds.
- Fraud rule blocks payment.

## Merchant Withdrawal Flow

1. Merchant selects withdraw business balance.
2. Merchant enters amount and destination.
3. System validates:
   - Merchant KYB is approved.
   - Merchant is active.
   - Business balance is sufficient.
   - Withdrawal destination is verified.
   - Amount is within withdrawal limits.
4. Transaction Service marks withdrawal as pending.
5. Ledger Service posts a balanced pending-withdrawal journal:
   - Debit merchant business balance account.
   - Credit external payout clearing account.
6. Payment Service emits a simulated payout callback.
7. If payout succeeds, Transaction Service marks withdrawal as completed and Wallet Service clears pending balance from ledger/payment events.
8. If payout fails, Ledger Service posts a reversal journal that debits payout clearing and credits merchant business balance, then Transaction Service marks withdrawal as failed.
9. Merchant sees withdrawal in history.

MVP uses pending withdrawal followed by simulated payout callback. The pending state is backed by ledger movement into a clearing account, not by a balance-only hold.

## Refund Flow

Refunds are included in MVP.

MVP flow:

1. Merchant opens a completed payment.
2. Merchant requests full refund.
3. System validates refund eligibility.
4. Refund enters admin approval queue.
5. Admin approves or rejects refund request.
6. If approved, Ledger posts reversal journal:
   - Debit merchant business balance account for the original merchant net amount.
   - Debit fee revenue account for the original flat fee amount.
   - Credit customer wallet account for the original gross payment amount.
7. Original transaction remains immutable.
8. Refund transaction links to original payment.

Rules:

- Never delete or mutate the original ledger entries.
- Use reversal entries.
- MVP supports full refund first.
- Partial refund can be added later.
- Refund appears as a separate transaction linked to the original payment.
- Refund ledger entries are posted only after admin approval.
- Full refund reverses both the merchant net credit and the platform fee revenue credit from the original merchant payment journal so the customer receives the original gross payment amount.
- Merchant balance sufficiency is checked against the merchant net amount, not against the fee revenue reversal.

## Admin Review Flow

Admin review queues:

- Pending customer KYC.
- Pending merchant KYB.
- Resubmissions.
- Fraud alerts.
- Refund approvals.
- Frozen account appeals.

Admin decisions:

- Approve.
- Reject with reason.
- Request resubmission with reason.
- Escalate to compliance reviewer.
- Freeze or unfreeze account.
- Approve or reject refund request.
- Resolve fraud review and retry blocked transaction.

Every admin decision must create an audit log.

## Fraud And Risk Flow

Fraud Service should start with explainable rules:

- Block transaction if wallet is frozen.
- Block unusually large transactions for review.
- Flag many transactions in a short time window.
- Flag repeated failed payment attempts.
- Flag suspicious merchant payment patterns.
- Flag withdrawal shortly after unusual incoming volume.

Risk outcomes:

- `ALLOW`: continue transaction.
- `REVIEW`: block transaction and create admin alert.
- `BLOCK`: fail transaction and create alert.

For MVP, use `ALLOW`, `REVIEW`, and `BLOCK`. `REVIEW` is blocking, but admin can resolve the alert and retry the blocked transaction. Add `HOLD` later if pre-authorization manual review is needed.

## Business Reporting Flow

Admin reports:

- Daily active customers.
- KYC approval and rejection counts.
- Merchant KYB approval and rejection counts.
- Top-up volume.
- Transfer volume.
- Merchant payment volume.
- Withdrawal volume.
- Refund volume.
- Fee revenue.
- Failed transaction count.
- Fraud alert count.

Merchant reports:

- Incoming payment list.
- Daily gross payment volume.
- Withdrawal history.
- Refund history.
- Basic settlement summary.

Customer reports:

- Transaction history.
- Monthly spending by type.
- Receipts.

## Notification Flow

Notifications should be created for:

- KYC submitted.
- KYC approved/rejected/resubmission required.
- KYB submitted.
- KYB approved/rejected/resubmission required.
- Top-up success/failure.
- Transfer sent/received.
- Merchant payment success/failure.
- Withdrawal success/failure.
- Refund requested/completed/failed.
- Account frozen/unfrozen.
- Suspicious activity warning.

Notification delivery can start as in-app only. Push notification can be added after core flows work.

## MVP Flow Priority

Build in this order:

1. Sign-in and profile creation.
2. Customer KYC.
3. Merchant KYB.
4. Admin approval dashboard.
5. Wallet activation after approval.
6. Top-up.
7. Customer transfer.
8. Merchant QR payment.
9. Merchant withdrawal.
10. Refunds.
11. Fraud flags.
12. Reports.

## Sources

- DANA Premium upgrade flow: https://www.dana.id/help-center/akun-profil/bagaimana-cara-menjadi-akun-dana-premium?lng=id
- DANA Bisnis microbusiness features: https://www.dana.id/business/microbusiness
- DANA QRIS merchant help: https://www.dana.id/en/mikrobisnis/qr
- Bank Indonesia QRIS education material: https://www.bi.go.id/id/edukasi/Documents/Bahan-Sosialisasi-QRIS.pdf
- Bank Indonesia sectoral risk assessment for non-bank e-money and e-wallet issuers: https://www.bi.go.id/en/fungsi-utama/sistem-pembayaran/anti-pencucian-uang-dan-pencegahan-pendanaan-terrorisme/Documents/SRA_en.pdf
