# ADR 0002: Business Flow Baseline

## Status

Accepted as initial business baseline.

## Context

FinFlow needs clear business behavior before implementation because wallet, ledger, KYC/KYB, merchant payment, refund, fraud, and reporting flows affect service boundaries and data models.

The product is inspired by Indonesian e-wallet and QRIS-style merchant payment behavior, while remaining a portfolio/product prototype rather than a licensed financial product.

## Decision

FinFlow MVP will use these business rules:

- Customer and merchant balances show available balance and pending balance.
- Merchant withdrawals enter pending state and complete through a simulated payout callback.
- Merchant QR payment amount is fixed by the merchant.
- Customer-to-customer transfer uses QR in MVP.
- Merchant payment includes configurable flat-fee accounting.
- KYC and KYB lock after repeated rejection.
- KYC and KYB rejection lock limit defaults to 3.
- Fraud `REVIEW` blocks the transaction and creates an admin alert.
- Admin can retry a fraud-review-blocked transaction after resolving the alert.
- Refunds are included in MVP.
- MVP refund support starts with full refunds for completed merchant payments.
- Refunds require admin approval.

## Consequences

Positive:

- Pending balances make withdrawal and payout states visible without pretending all money movement is instant.
- QR-only customer transfer keeps mobile scope focused and demonstrates scanner/generator flows.
- Configurable flat merchant fees make double-entry ledger accounting more realistic.
- Blocking fraud review is safer and easier to reason about than allowing high-risk transactions to complete.
- Refunds force the ledger model to support reversals from the start.

Negative:

- Refunds, fees, pending balances, and payout callbacks increase MVP complexity.
- QR-only transfers are narrower than a real wallet app with phone or username lookup.
- Rejection locks require extra admin unlock behavior and audit logging.

## Guardrails

- Do not mutate original transaction or ledger entries during refund.
- Do not complete merchant withdrawal synchronously without the simulated payout callback.
- Do not let blocked fraud review transactions post ledger entries.
- Do not retry a fraud-review-blocked transaction without an audited admin resolution.
- Do not implement phone or username transfer lookup in the first MVP.
- Do not hardcode merchant payment fees; the flat amount must be configurable.
- Do not post refund ledger reversals before admin approval.
