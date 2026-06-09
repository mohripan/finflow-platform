# ADR 0005: Customer KYC Evidence MVP

## Status

Accepted

## Context

The onboarding MVP needs document and selfie capture without pretending to provide production-grade biometric liveness or automated face matching. KYC Service owns customer verification state, while object storage owns document bytes. Admin review must use real uploaded evidence and short-lived review URLs.

## Decision

Customer KYC submission is split into:

1. Create or update a `DRAFT` KYC application with structured identity metadata.
2. Create signed upload sessions for `IDENTITY_DOCUMENT` and `SELFIE`.
3. Upload image bytes directly to MinIO using the signed URL.
4. Confirm each uploaded object through KYC Service, which verifies the object exists and matches the expected size before marking evidence `UPLOADED`.
5. Submit the application for admin review only after both required evidence files are uploaded.

Admin users review evidence metadata in the dashboard and request short-lived signed review URLs from KYC Service. Signed URLs and document bytes are never emitted in events and are not stored in PostgreSQL.

Automated liveness and face matching remain out of scope for this MVP. The selfie is collected as review evidence, not as a biometric acceptance decision.

## Consequences

- The mobile and admin apps use real backend contracts instead of mock production paths.
- KYC Service stores document metadata and object keys, but not document bytes.
- Final submission fails if identity document or selfie evidence is missing.
- Admin review can inspect uploaded evidence before approving, rejecting, or requesting resubmission.
- A later biometric provider integration can add liveness and face-match verdicts without changing the core review boundary.
