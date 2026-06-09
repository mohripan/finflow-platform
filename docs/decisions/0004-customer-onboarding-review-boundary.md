# ADR 0004: Customer Onboarding Review Boundary

## Status

Accepted

## Context

Customer onboarding spans User Service and KYC Service. KYC Service owns identity verification applications and admin review decisions. User Service owns the customer account lifecycle that gates wallet eligibility. The platform does not yet include Wallet Service or Ledger Service, so wallet activation cannot be completed without creating fake production behavior.

## Decision

KYC Service implements customer KYC submission, admin review queue, and admin decisions. Each admin decision writes an append-only local decision record. After a terminal decision, KYC Service calls User Service through `POST /api/v1/users/internal/customer-status` to synchronize the customer lifecycle state.

Approved KYC moves the customer account to `KYC_APPROVED`. Wallet activation remains a later Wallet Service workflow and must not be simulated in KYC Service, User Service, mobile, or admin UI.

## Consequences

- Service ownership stays explicit: KYC owns review state; User owns customer lifecycle state.
- No service reads another service database.
- Admin approval is demoable end to end up to wallet eligibility.
- A later Wallet Service slice can subscribe to or query the approved lifecycle state and create wallets with ledger accounts.
- The admin dashboard and mobile app show truthful onboarding state instead of pretending a wallet exists.
