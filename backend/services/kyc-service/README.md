# KYC Service

Owns customer KYC application state and sensitive identity intake.

Implemented slice:

- `GET /api/v1/kyc/me` returns the authenticated actor's KYC status.
- `POST /api/v1/kyc/me/submissions` validates and persists KYC metadata.
- National identity numbers are hashed immediately and are never returned by APIs.

Document upload and admin decision workflows are intentionally left for the next onboarding slice because they require MinIO metadata verification and audit tables.

Verification:

```text
mvn -pl backend/services/kyc-service test
```
