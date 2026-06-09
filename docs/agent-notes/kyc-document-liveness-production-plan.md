# KYC Document And Liveness Production Plan

## Why This Is A Separate Slice

Document capture, selfie liveness, and face matching are not just mobile UI features. They add sensitive document storage, biometric processing, vendor risk, replay protection, admin evidence review, retention rules, and audit requirements.

FinFlow should not hand-roll production liveness detection. Biometric presentation attack detection should be designed around ISO/IEC 30107 concepts and evaluated vendor signals, with NIST FATE PAD-style performance evidence used when selecting a provider.

References:

- ISO/IEC 30107-1:2023, biometric presentation attack detection framework: https://www.iso.org/standard/83828.html
- NIST FATE PAD evaluation program: https://pages.nist.gov/frvt/html/frvt_pad.html

## Target Flow

1. Customer signs up or logs in through Keycloak.
2. Mobile app collects KYC metadata.
3. KYC Service creates a draft application.
4. Mobile app requests signed upload URLs for required evidence:
   - identity document front image, KTP or SIM
   - selfie or liveness capture package
5. Mobile app uploads bytes directly to MinIO/S3 using signed URLs.
6. Mobile app confirms uploaded evidence metadata to KYC Service.
7. KYC Service calls an Identity Verification adapter.
8. Adapter returns structured verification signals:
   - document OCR status
   - document authenticity status
   - face match score or status
   - liveness/PAD status
   - provider reference
   - error/risk reason codes
9. KYC Service stores only metadata, object references, hashes, and verification results.
10. Admin dashboard shows application, document thumbnails through short-lived review URLs, verification signals, and decision history.
11. Admin approves, rejects, or requests resubmission.

## Backend Design

Add KYC domain models:

- `KycDocument`
- `KycEvidenceUploadSession`
- `KycVerificationAttempt`
- `KycVerificationSignal`

Add application ports:

- `DocumentStoragePort`
- `IdentityVerificationPort`
- `ReviewEvidenceUrlPort`

Add infrastructure adapters:

- `MinioDocumentStorageAdapter`
- `NoopIdentityVerificationAdapter` for local development only, returning `MANUAL_REVIEW_REQUIRED`, never auto-approving.
- Later: vendor adapter such as Onfido, Veriff, Sumsub, AWS Rekognition Face Liveness, or another provider selected by legal/compliance and performance evidence.

Important rule:

- Local development may simulate provider unavailability or manual-review-required status, but must not fake a successful liveness or face match.

## API Additions

KYC Service:

- `POST /api/v1/kyc/me/applications`
- `POST /api/v1/kyc/me/applications/{applicationId}/documents/upload-sessions`
- `POST /api/v1/kyc/me/applications/{applicationId}/documents/{documentId}/confirm-upload`
- `POST /api/v1/kyc/me/applications/{applicationId}/verification-attempts`
- `GET /api/v1/kyc/admin/applications/{applicationId}/evidence`
- `POST /api/v1/kyc/admin/applications/{applicationId}/evidence/{documentId}/review-url`

## Mobile Design

Use a multi-step flow:

1. Welcome and auth choice.
2. Personal details.
3. Identity document capture.
4. Selfie/liveness capture.
5. Review and submit.
6. Pending review status.

Recommended Expo modules:

- `expo-camera` for controlled capture.
- `expo-image-manipulator` for size normalization before upload.
- `expo-file-system` only for transient local file handling.

Mobile must not:

- Store document images permanently on device.
- Log document paths or identity numbers.
- Claim liveness passed unless backend verification returns a real provider result.

## Admin Dashboard Design

Admin review should show:

- KYC metadata.
- Document evidence list.
- Short-lived review thumbnails.
- Liveness and face-match status.
- Provider reference and risk reason codes.
- Decision history.

Admin decisions must include a reason and must write local audit/decision history.

## Tests

Backend:

- signed upload session creation
- invalid content type rejection
- confirm upload with checksum mismatch
- verification attempt idempotency
- provider timeout creates manual review status
- admin review URL expires
- decision history append-only

Mobile:

- wizard navigation
- camera permission denied state
- capture success with local preview
- upload failure and retry
- successful evidence confirmation

E2E:

- customer submits metadata and uploads evidence to MinIO
- KYC verification returns manual review required locally
- admin reviews evidence and approves
- customer lifecycle becomes `KYC_APPROVED`

## Open Questions

- Which vendor or cloud provider is acceptable for production biometric verification?
- What retention period applies to KTP/SIM images and selfie/liveness evidence?
- Are customers allowed to delete/revoke biometric evidence after approval?
- Should verification failure auto-reject or always go to manual review in MVP?
