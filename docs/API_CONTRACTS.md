# FinFlow API Contracts

## Purpose

This document defines the first REST API contract for FinFlow before OpenAPI files are generated. It describes external and internal HTTP APIs, request/response conventions, roles, idempotency rules, error behavior, and service ownership.

These contracts are intentionally implementation-facing. They should later be converted into OpenAPI specs under `contracts/openapi/`.

## OpenAPI Conversion Rules

When implementation starts, each gateway-facing service API must have an OpenAPI spec under:

```text
contracts/openapi/<service-name>/openapi.yaml
```

Rules:

- Specs must use OpenAPI 3.1 unless tooling limitations require OpenAPI 3.0.
- The prose contract in this document remains the baseline until a service-specific OpenAPI file exists.
- Once an OpenAPI file exists, changes to endpoint paths, request bodies, response bodies, status codes, headers, auth roles, or error codes must update both the service implementation and the spec in the same change.
- Public path parameters must be named with public identifier semantics, for example `merchantPublicId`, `walletPublicId`, or `transactionPublicReference`.
- Every operation must include the common success or error envelope shape.
- Every money-moving command must document the required `Idempotency-Key` header.
- Every authenticated operation must document bearer-token security.
- Error responses must reference shared error envelope components and list expected business error codes where practical.
- Generated clients may be produced from OpenAPI specs, but generated artifacts should not be committed unless a later build decision explicitly requires it.
- CI should lint OpenAPI files and fail on invalid specs once `contracts/openapi/` exists.

Recommended shared components:

- `ApiResponse`
- `ApiError`
- `ApiMeta`
- `PageInfo`
- `MoneyDto`
- `AuditDecisionDto`
- `DocumentMetadataDto`

## API Principles

1. All public APIs are routed through Spring Cloud Gateway.
2. Services own their own APIs and data.
3. Gateway validates authentication and coarse role access.
4. Services enforce domain authorization and business rules.
5. Money-moving commands require an idempotency key.
6. Public APIs expose `publicId` or `publicReference`, not database IDs.
7. Timestamps use ISO-8601 UTC.
8. Money uses integer minor units plus currency.
9. Errors use one shared envelope.
10. File bytes use object upload flows; service databases store metadata only.

## Identifier Rules

Route parameters in public APIs are public-safe identifiers, even when the placeholder uses a short name such as `{merchantId}` or `{transactionId}`.

Rules:

- Public API IDs must use the entity public ID or public reference, for example `mrc_...`, `txn_...`, `wal_...`, `kyc_...`, `jrn_...`.
- Internal UUID primary keys stay inside the owning service database and are not accepted in public routes.
- Internal service commands may carry internal IDs only when both services have a documented command contract and the IDs were previously exchanged through trusted workflow state.
- OpenAPI files must name path parameters clearly, for example `merchantPublicId`, `transactionPublicReference`, or `walletPublicId`, even if prose examples keep shorter labels.

## Base URLs

Local gateway:

```text
http://localhost:8080/api
```

Versioning:

```text
/api/v1/...
```

Internal service routes should remain behind the gateway in local and deployed environments unless explicitly marked as internal.

## Common Headers

Required for authenticated requests:

```http
Authorization: Bearer <access-token>
X-Correlation-Id: <uuid>
```

Required for money-moving commands:

```http
Idempotency-Key: <client-generated-key>
```

Recommended:

```http
Accept-Language: en-US
```

Rules:

- Gateway creates `X-Correlation-Id` if absent.
- Services must propagate `X-Correlation-Id`.
- Idempotency keys are scoped by actor, endpoint/command, and request hash.
- Reusing the same idempotency key with the same payload returns the original result.
- Reusing the same idempotency key with a different payload returns `409 CONFLICT`.

## Roles

Keycloak roles:

- `CUSTOMER`
- `MERCHANT`
- `ADMIN`
- `COMPLIANCE`
- `SUPPORT`
- `SERVICE`

Rules:

- `CUSTOMER` can access own customer wallet and transaction APIs.
- `MERCHANT` can access owned merchant resources.
- `ADMIN` can review KYC/KYB, refunds, fraud, accounts, and reports.
- `COMPLIANCE` can unlock KYC/KYB and review sensitive risk cases.
- `SERVICE` is for internal service-to-service calls.

## Common Response Envelope

Single-resource success:

```json
{
  "data": {},
  "meta": {
    "correlationId": "018f7f3f-2c6c-7d1b-9b77-3e4f1a5e9c10"
  }
}
```

List success:

```json
{
  "data": [],
  "page": {
    "size": 20,
    "nextCursor": "opaque-cursor",
    "hasMore": true
  },
  "meta": {
    "correlationId": "018f7f3f-2c6c-7d1b-9b77-3e4f1a5e9c10"
  }
}
```

Error:

```json
{
  "error": {
    "code": "WALLET_FROZEN",
    "message": "Wallet is frozen and cannot initiate outgoing transactions.",
    "details": {
      "walletId": "wal_..."
    }
  },
  "meta": {
    "correlationId": "018f7f3f-2c6c-7d1b-9b77-3e4f1a5e9c10"
  }
}
```

## Common HTTP Statuses

| Status | Meaning |
| --- | --- |
| `200 OK` | Successful query or idempotent replay |
| `201 CREATED` | Resource created |
| `202 ACCEPTED` | Command accepted for async workflow |
| `400 BAD_REQUEST` | Invalid syntax or validation failure |
| `401 UNAUTHORIZED` | Missing or invalid token |
| `403 FORBIDDEN` | Authenticated but not allowed |
| `404 NOT_FOUND` | Resource not found or hidden |
| `409 CONFLICT` | State conflict, duplicate, idempotency mismatch |
| `422 UNPROCESSABLE_ENTITY` | Business rule prevents operation |
| `429 TOO_MANY_REQUESTS` | Rate limit |
| `500 INTERNAL_SERVER_ERROR` | Unexpected server error |

## Common Error Codes

| Code | Usage |
| --- | --- |
| `VALIDATION_ERROR` | Request field validation failed |
| `UNAUTHORIZED` | Missing/invalid auth |
| `FORBIDDEN` | Role or ownership denial |
| `RESOURCE_NOT_FOUND` | Resource not found |
| `IDEMPOTENCY_CONFLICT` | Same key used with different request |
| `KYC_NOT_APPROVED` | Customer cannot transact |
| `KYB_NOT_APPROVED` | Merchant cannot receive/withdraw |
| `WALLET_FROZEN` | Wallet blocks outgoing movement |
| `INSUFFICIENT_FUNDS` | Available balance is too low |
| `QR_EXPIRED` | QR request expired |
| `QR_ALREADY_PAID` | QR request already consumed |
| `FRAUD_REVIEW_REQUIRED` | Fraud review blocks transaction |
| `FRAUD_BLOCKED` | Fraud rule blocks transaction |
| `LEDGER_UNBALANCED` | Journal is invalid |
| `REFUND_REQUIRES_ADMIN_APPROVAL` | Refund cannot post yet |
| `REJECTION_LIMIT_REACHED` | KYC/KYB locked after repeated rejection |

## Shared DTOs

### MoneyDto

```json
{
  "amountMinor": 100000,
  "currency": "IDR"
}
```

### AuditDecisionDto

```json
{
  "decision": "APPROVE",
  "reason": "Documents match submitted profile."
}
```

### DocumentMetadataDto

```json
{
  "documentId": "doc_01HT...",
  "documentType": "IDENTITY_CARD",
  "contentType": "image/jpeg",
  "sizeBytes": 541223,
  "status": "UPLOADED",
  "createdAt": "2026-06-08T10:15:30Z",
  "updatedAt": "2026-06-08T10:15:30Z"
}
```

## User Service API

Base route:

```text
/api/v1/users
```

### `GET /me`

Roles:

- `CUSTOMER`
- `MERCHANT`
- `ADMIN`
- `SUPPORT`
- `COMPLIANCE`

Returns the current application profile, creating it idempotently if this is the user's first authenticated request.

Response:

```json
{
  "data": {
    "userId": "usr_01HT...",
    "email": "user@example.com",
    "phoneNumber": "+6281234567890",
    "displayName": "Ayu",
    "status": "ACTIVE",
    "customerAccount": {
      "customerId": "cus_01HT...",
      "status": "REGISTERED"
    },
    "createdAt": "2026-06-08T10:15:30Z",
    "updatedAt": "2026-06-08T10:15:30Z"
  }
}
```

### `PATCH /me`

Roles:

- `CUSTOMER`
- `MERCHANT`

Request:

```json
{
  "displayName": "Ayu Lestari",
  "preferredLanguage": "id-ID"
}
```

### `GET /admin/users`

Roles:

- `ADMIN`
- `SUPPORT`

Query:

- `status`
- `email`
- `phoneNumber`
- `cursor`
- `size`

### `PATCH /admin/users/{userId}/status`

Roles:

- `ADMIN`

Request:

```json
{
  "status": "SUSPENDED",
  "reason": "Account under review."
}
```

## KYC Service API

Base route:

```text
/api/v1/kyc
```

### `GET /me`

Roles:

- `CUSTOMER`
- `MERCHANT`

Returns current user's KYC application status.

### `POST /me/submissions`

Roles:

- `CUSTOMER`
- `MERCHANT`

Request:

```json
{
  "legalName": "Ayu Lestari",
  "dateOfBirth": "1996-05-20",
  "nationalIdentityNumber": "3173...",
  "phoneNumber": "+6281234567890",
  "address": "Jakarta Selatan"
}
```

Response status:

- `202 ACCEPTED`

Rules:

- Reject with `REJECTION_LIMIT_REACHED` if KYC is locked.
- Sensitive identity values are accepted only over TLS, are immediately hashed and encrypted by KYC Service, are never logged, and are never returned by APIs or emitted in Kafka events.

### `POST /me/documents`

Roles:

- `CUSTOMER`
- `MERCHANT`

Creates document upload metadata and returns a short-lived signed object upload URL.

Request:

```json
{
  "documentType": "IDENTITY_CARD",
  "contentType": "image/jpeg",
  "sizeBytes": 541223,
  "checksum": "sha256:..."
}
```

Response:

```json
{
  "data": {
    "documentId": "doc_01HT...",
    "uploadUrl": "http://localhost:9000/...",
    "uploadMethod": "PUT",
    "requiredHeaders": {
      "Content-Type": "image/jpeg",
      "x-amz-checksum-sha256": "..."
    },
    "expiresAt": "2026-06-08T10:25:30Z"
  }
}
```

Rules:

- KYC/KYB document uploads use signed object URLs as the production-shaped path for MVP.
- Application services store document metadata and object references only; they do not persist document bytes in PostgreSQL and do not proxy file bytes through normal JSON APIs.
- Signed upload URLs must be short-lived, scoped to one object key, content type, size limit, checksum, actor, and document type.
- After upload, the owning service verifies object metadata/checksum before the document can be reviewed.
- Signed object URLs must never be included in Kafka events.

### `GET /admin/applications`

Roles:

- `ADMIN`
- `COMPLIANCE`

Query:

- `status`
- `cursor`
- `size`

### `POST /admin/applications/{applicationId}/decision`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `applicationId`: KYC application public ID.

Request:

```json
{
  "decision": "APPROVE",
  "reason": "Identity document is valid."
}
```

Decision values:

- `APPROVE`
- `REJECT`
- `REQUEST_RESUBMISSION`

### `POST /admin/applications/{applicationId}/unlock`

Roles:

- `COMPLIANCE`

Path parameters:

- `applicationId`: KYC application public ID.

Request:

```json
{
  "reason": "Manual review allows resubmission."
}
```

## Merchant Service API

Base route:

```text
/api/v1/merchants
```

### `POST /`

Roles:

- `MERCHANT`

Creates a merchant profile.

Request:

```json
{
  "merchantName": "Warung Ayu",
  "businessCategory": "FOOD_AND_BEVERAGE",
  "businessType": "INDIVIDUAL"
}
```

### `GET /me`

Roles:

- `MERCHANT`

Returns merchant profiles owned by current user.

### `POST /{merchantId}/kyb/submissions`

Roles:

- `MERCHANT`

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

Request:

```json
{
  "businessName": "Warung Ayu",
  "businessCategory": "FOOD_AND_BEVERAGE",
  "businessType": "INDIVIDUAL",
  "businessRegistrationNumber": null,
  "taxNumber": null,
  "businessAddress": "Jakarta Selatan"
}
```

Rules:

- Owner KYC must be approved before KYB submission is accepted. This avoids collecting business verification data for an owner who is not eligible to operate a merchant account.

### `POST /{merchantId}/kyb/documents`

Roles:

- `MERCHANT`

Same upload pattern as KYC documents.

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

### `POST /{merchantId}/withdrawal-destinations`

Roles:

- `MERCHANT`

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

Request:

```json
{
  "destinationType": "BANK_ACCOUNT",
  "accountHolderName": "Ayu Lestari",
  "bankCode": "BCA",
  "accountNumber": "1234567890"
}
```

Rules:

- Raw withdrawal destination account numbers are accepted only over TLS, are immediately masked, hashed, and encrypted where needed by Merchant Service, are never logged, and are never returned by APIs or emitted in Kafka events.

### `GET /{merchantId}/withdrawal-destinations`

Roles:

- `MERCHANT`

### `GET /admin/kyb/applications`

Roles:

- `ADMIN`
- `COMPLIANCE`

### `POST /admin/kyb/applications/{applicationId}/decision`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `applicationId`: KYB application public ID.

Request:

```json
{
  "decision": "APPROVE",
  "reason": "Business data and owner identity verified."
}
```

### `POST /admin/merchants/{merchantId}/suspend`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `merchantId`: merchant public ID.

Request:

```json
{
  "reason": "Suspicious payment pattern."
}
```

### `POST /admin/merchants/{merchantId}/reactivate`

Roles:

- `ADMIN`
- `COMPLIANCE`

## Wallet Service API

Base route:

```text
/api/v1/wallets
```

### `GET /me`

Roles:

- `CUSTOMER`

Returns customer wallet and balance.

Response:

```json
{
  "data": {
    "walletId": "wal_01HT...",
    "status": "ACTIVE",
    "availableBalance": {
      "amountMinor": 250000,
      "currency": "IDR"
    },
    "pendingBalance": {
      "amountMinor": 50000,
      "currency": "IDR"
    },
    "updatedAt": "2026-06-08T10:15:30Z"
  }
}
```

### `GET /merchant/{merchantId}`

Roles:

- `MERCHANT`

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

Returns merchant business wallet and available/pending balance.

### `POST /admin/{walletId}/freeze`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `walletId`: wallet public ID.

Request:

```json
{
  "reason": "Fraud investigation."
}
```

### `POST /admin/{walletId}/unfreeze`

Roles:

- `ADMIN`
- `COMPLIANCE`

## Transaction Service API

Base route:

```text
/api/v1/transactions
```

Money-moving command endpoints require `Idempotency-Key`.

### `POST /top-ups`

Roles:

- `CUSTOMER`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "amountMinor": 100000,
  "currency": "IDR"
}
```

Response:

```json
{
  "data": {
    "transactionId": "txn_01HT...",
    "publicReference": "FF-20260608-000001",
    "status": "PENDING_PAYMENT",
    "paymentInstructionId": "pay_01HT..."
  }
}
```

### `POST /transfers/qr-tokens`

Roles:

- `CUSTOMER`

Creates QR token for receiving transfer.

Request:

```json
{
  "expiresInSeconds": 600
}
```

Response:

```json
{
  "data": {
    "transferQrTokenId": "tqr_01HT...",
    "qrPayload": "finflow://transfer?t=...",
    "expiresAt": "2026-06-08T10:25:30Z"
  }
}
```

### `POST /transfers/qr-payments`

Roles:

- `CUSTOMER`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "qrPayload": "finflow://transfer?t=...",
  "amountMinor": 25000,
  "currency": "IDR",
  "note": "Lunch"
}
```

### `POST /merchant-payments/qr-requests`

Roles:

- `MERCHANT`

Request:

```json
{
  "merchantId": "mrc_01HT...",
  "amountMinor": 50000,
  "currency": "IDR",
  "orderReference": "ORDER-123"
}
```

Rules:

- Amount is fixed by merchant.
- Merchant must be active.
- `merchantId` is the merchant public ID.

### `POST /merchant-payments/pay`

Roles:

- `CUSTOMER`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "qrPayload": "finflow://merchant-payment?q=..."
}
```

Response:

```json
{
  "data": {
    "transactionId": "txn_01HT...",
    "publicReference": "FF-20260608-000002",
    "status": "COMPLETED",
    "amount": {
      "amountMinor": 50000,
      "currency": "IDR"
    },
    "merchantFee": {
      "amountMinor": 1000,
      "currency": "IDR"
    },
    "merchantNetAmount": {
      "amountMinor": 49000,
      "currency": "IDR"
    }
  }
}
```

### `POST /merchant-withdrawals`

Roles:

- `MERCHANT`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "merchantId": "mrc_01HT...",
  "withdrawalDestinationId": "wdd_01HT...",
  "amountMinor": 100000,
  "currency": "IDR"
}
```

Response status:

- `202 ACCEPTED`

Rules:

- Withdrawal enters `PENDING_PAYOUT`.
- Completion requires simulated payout callback from Payment Service.
- `merchantId` and `withdrawalDestinationId` are public IDs owned by the current merchant actor.

### `POST /refunds`

Roles:

- `MERCHANT`

Headers:

- `Idempotency-Key`

Request:

```json
{
  "originalTransactionId": "txn_01HT...",
  "reason": "Customer returned item."
}
```

Rules:

- `originalTransactionId` is the original merchant payment public transaction ID or public reference.
- Full refund reverses the original merchant payment journal: merchant net amount is debited from merchant balance, fee revenue is debited for the original flat fee, and customer wallet is credited for the gross payment amount.
- Merchant balance sufficiency is checked against the merchant net refund amount. Fee revenue reversal is a platform accounting reversal and is not charged again to the merchant.

Response:

```json
{
  "data": {
    "refundRequestId": "rfd_01HT...",
    "status": "PENDING_ADMIN_APPROVAL"
  }
}
```

### `POST /admin/refunds/{refundRequestId}/decision`

Roles:

- `ADMIN`
- `COMPLIANCE`

Request:

```json
{
  "decision": "APPROVE",
  "reason": "Merchant evidence accepted."
}
```

Decision values:

- `APPROVE`
- `REJECT`

Path parameters:

- `refundRequestId`: refund request public ID.

### `GET /me`

Roles:

- `CUSTOMER`

Query:

- `type`
- `status`
- `cursor`
- `size`

Returns current customer's transaction history.

### `GET /merchant/{merchantId}`

Roles:

- `MERCHANT`

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

Returns merchant transaction history.

### `GET /admin/transactions`

Roles:

- `ADMIN`
- `SUPPORT`
- `COMPLIANCE`

Query:

- `type`
- `status`
- `publicReference`
- `from`
- `to`
- `cursor`
- `size`

## Payment Service API

Payment Service APIs are internal/admin simulation APIs for MVP.

Base route:

```text
/api/v1/payments
```

### `POST /simulation/payments/{paymentInstructionId}/success`

Roles:

- `ADMIN`
- `SERVICE`

Path parameters:

- `paymentInstructionId`: payment instruction public ID.

Simulates successful top-up payment.

### `POST /simulation/payments/{paymentInstructionId}/fail`

Roles:

- `ADMIN`
- `SERVICE`

Request:

```json
{
  "reason": "Simulated payment failure."
}
```

### `POST /simulation/payouts/{payoutInstructionId}/callback`

Roles:

- `ADMIN`
- `SERVICE`

Request:

```json
{
  "status": "COMPLETED",
  "providerReference": "SIM-PAYOUT-123",
  "reason": null
}
```

Rules:

- Duplicate callbacks are idempotent.
- Callback is required for withdrawal completion.

## Fraud Service API

Base route:

```text
/api/v1/fraud
```

### `GET /admin/alerts`

Roles:

- `ADMIN`
- `COMPLIANCE`

Query:

- `status`
- `decision`
- `subjectType`
- `cursor`
- `size`

### `POST /admin/alerts/{alertId}/resolve`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `alertId`: fraud alert public ID.

Request:

```json
{
  "resolution": "APPROVE_RETRY",
  "reason": "False positive after manual review."
}
```

Resolution values:

- `APPROVE_RETRY`
- `DISMISS`
- `CONFIRM_BLOCK`

### `POST /admin/alerts/{alertId}/retry`

Roles:

- `ADMIN`
- `COMPLIANCE`

Requests retry of the original fraud-review-blocked transaction.

Rules:

- Alert must be resolved with `APPROVE_RETRY`.
- Transaction Service re-runs all current validations.

## Ledger Service API

Ledger public APIs are read-only/admin-oriented for MVP. Journal posting should be internal command/workflow behavior.

Base route:

```text
/api/v1/ledger
```

### `GET /admin/journals`

Roles:

- `ADMIN`
- `COMPLIANCE`

Query:

- `transactionId`
- `publicReference`
- `journalType`
- `from`
- `to`
- `cursor`
- `size`

### `GET /admin/journals/{journalId}`

Roles:

- `ADMIN`
- `COMPLIANCE`

Path parameters:

- `journalId`: ledger journal public ID.

Returns journal and entries.

### `POST /internal/journals`

Roles:

- `SERVICE`

Operational fallback endpoint only. Production financial workflows must post journals through the Ledger Service Axon command handler.

Rules:

- Must reject unbalanced journals.
- Must be idempotent by transaction step reference.

## Notification Service API

Base route:

```text
/api/v1/notifications
```

### `GET /me`

Roles:

- `CUSTOMER`
- `MERCHANT`
- `ADMIN`

Query:

- `status`
- `cursor`
- `size`

### `POST /{notificationId}/read`

Roles:

- `CUSTOMER`
- `MERCHANT`
- `ADMIN`

Marks notification as read if owned by current actor.

## Reporting Service API

Base route:

```text
/api/v1/reports
```

### `GET /admin/overview`

Roles:

- `ADMIN`
- `COMPLIANCE`

Query:

- `from`
- `to`
- `currency`

Returns high-level operational metrics.

### `GET /admin/transactions/daily`

Roles:

- `ADMIN`
- `COMPLIANCE`

### `GET /admin/fees`

Roles:

- `ADMIN`
- `COMPLIANCE`

### `GET /merchant/{merchantId}/settlement-summary`

Roles:

- `MERCHANT`

Path parameters:

- `merchantId`: merchant public ID owned by the current actor.

Query:

- `from`
- `to`

Returns merchant incoming payments, withdrawals, refunds, and fee totals.

## Internal Workflow Commands

Financial workflow decisions use Axon command/query contracts, not public REST endpoints. These command/query contracts must not be exposed to mobile/web clients.

### Wallet Status Validation

Command/query:

```text
ValidateWalletOutgoingStatus
```

Request:

```json
{
  "walletId": "wal_01HT...",
  "purpose": "MERCHANT_PAYMENT"
}
```

Rules:

- Wallet Service validates wallet ownership/status only.
- Ledger Service, not Wallet Service, makes the authoritative balance sufficiency decision during journal posting.

### Merchant Active Validation

Command/query:

```text
ValidateMerchantActiveForPayment
```

Rules:

- Merchant Service validates merchant KYB approval, active status, suspension state, and payment acceptance settings at payment confirmation time.
- Event-cached merchant state can be used only for UI pre-checks.

### Fraud Evaluation

Command/query:

```text
EvaluateTransactionRisk
```

Request:

```json
{
  "transactionId": "txn_01HT...",
  "type": "MERCHANT_PAYMENT",
  "actorId": "usr_01HT...",
  "merchantId": "mrc_01HT...",
  "amountMinor": 50000,
  "currency": "IDR"
}
```

Rules:

- Transaction workflow must receive `ALLOW`, `REVIEW`, or `BLOCK` before requesting ledger posting.
- Timeout or unavailable Fraud Service fails closed and does not post ledger entries.

### Ledger Journal Posting

Command:

```text
PostLedgerJournal
```

Rules:

- Ledger Service validates balanced entries, account status, idempotent transaction step reference, and authoritative account balance inside one serialized posting transaction.
- Ledger Service rejects insufficient funds; Wallet Service projections are never used as the financial source of truth.

## Document Review URLs

KYC Service and Merchant Service generate short-lived signed document review URLs for authorized admin/compliance users.

Rules:

- Review URLs are generated only after service-level authorization and ownership checks.
- Review URLs are scoped to one object key, actor, document ID, content type, and short expiration.
- Review URLs are returned only through admin APIs and are never emitted in Kafka events.
- A dedicated document-access service is not part of the baseline. Adding one later requires an ADR because it changes sensitive document access ownership.

## Contract Testing Requirements

OpenAPI:

- Each service owns its OpenAPI document.
- CI validates OpenAPI syntax.
- Gateway route tests verify documented routes exist.
- Frontend clients should be generated or checked against OpenAPI where practical.

Runtime:

- Controller tests must verify status codes and error envelopes.
- Money-moving endpoint tests must verify idempotency behavior.
- Admin endpoints must verify role restrictions.
- Document upload endpoints must verify object metadata and signed URL behavior.

## API Decisions

These decisions are fixed before OpenAPI generation:

1. KYC Service and Merchant Service generate signed document upload and review URLs for their own documents.
2. Financial validation and journal posting use Axon command/query contracts. Internal REST is not the default path for money-moving decisions.
3. Admin transaction search avoids raw PII filters in the baseline. Search by public reference, status, type, date range, merchant public ID, customer public ID, or masked identifiers only.
4. Mobile and admin clients call gateway routes to owning services for the baseline. A BFF may be added later only when repeated cross-service client aggregation causes measurable complexity.
