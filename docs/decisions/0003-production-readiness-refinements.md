# ADR 0003: Production-Readiness Refinements Before Implementation

## Status

Accepted.

## Context

The initial documentation established FinFlow's product flows, service boundaries, API contracts, event contracts, and roadmap. A pre-implementation review found several areas that needed production-grade decisions before code generation:

- Account lifecycle statuses and KYC/KYB review statuses were easy to conflate.
- Internal workflow communication allowed both REST and Axon for financial decisions.
- Authoritative balance validation needed one clear owner.
- Merchant payment refund accounting did not specify how fee revenue is reversed.
- Open API, boundary, and event questions remained despite the Q&A saying none were open.
- The first money-movement phase was too broad to implement safely as one slice.

## Decision

FinFlow will use these refinements:

- Customer account lifecycle, merchant account lifecycle, KYC application review, and KYB application review are separate state machines.
- Public APIs expose public IDs and public references only. Internal UUID primary keys remain inside owning service databases.
- Internal financial workflow decisions use Axon command/query semantics with bounded responses. Internal REST is reserved for gateway-facing APIs, non-financial lookups, and operational support endpoints.
- Ledger Service owns authoritative balance sufficiency during journal posting. Wallet Service owns status and projection pre-checks only.
- Ledger journal posting serializes affected ledger accounts and calculates balances from posted ledger entries in the same database transaction as journal insertion.
- System ledger accounts for payment clearing, payout clearing, fee revenue, and suspense are bootstrapped idempotently per currency before money movement is enabled.
- Full merchant payment refunds reverse the original gross customer payment by debiting merchant business balance for the merchant net amount, debiting fee revenue for the original flat fee, and crediting the customer wallet for the gross amount.
- Local per-service audit tables are mandatory and remain the audit source of truth. Centralized audit Kafka events are published through outbox for search and reporting.
- KYC Service and Merchant Service generate signed upload/review URLs for documents they own.
- Kafka uses one topic per domain, per-domain Avro contract modules plus a common module, and non-sensitive ledger entry summaries in `LedgerJournalPosted`.
- Money movement delivery is split into top-up, customer transfer, merchant QR payment, and merchant withdrawal/refund vertical slices.

## Consequences

Positive:

- Implementation has one clear owner for financial balance correctness.
- Service communication is more consistent for financial workflows.
- Refunds are accounting-complete from the start.
- Frontend and OpenAPI generation can consistently use public identifiers.
- Ledger-derived projections do not need cross-service database reads or routine Ledger API calls.
- Smaller money-movement slices reduce delivery risk while preserving production-grade behavior.

Negative:

- Axon command/query contracts must be designed carefully before financial workflows are implemented.
- Ledger Service must handle account-level serialization and insufficient-funds checks early.
- Full refund reverses platform fee revenue, which may differ from a later commercial fee-retention policy.
- Per-domain contract modules add build setup work in Phase 0.

## Guardrails

- Do not let Wallet Service authorize balance sufficiency for money movement.
- Do not use Kafka events as command messages for financial decisions.
- Do not expose internal UUID primary keys in public APIs.
- Do not collapse account lifecycle and verification application statuses into one enum.
- Do not keep merchant payment fee revenue during full refund unless a later ADR explicitly changes refund policy.
- Do not add a dedicated document-access service without an ADR because it changes sensitive document ownership.
