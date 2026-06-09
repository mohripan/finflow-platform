# Customer Onboarding Agent Notes

## What Changed

- Refactored KYC Service into `api`, `application`, `domain`, and `infrastructure` packages.
- Refactored User Service into `api`, `application`, `domain`, and `infrastructure` packages.
- Added use-case interfaces for KYC onboarding and user profile lifecycle operations.
- Split the mobile app into API, config, type, component, style, and screen modules under `mobile/app/src`.
- Split the admin dashboard into API, auth, type, component, and screen modules under `admin/dashboard/src`.
- Updated the mobile/admin primary color direction to Docker/Kubernetes-style blue.
- Added the mobile app icon asset to Expo config and the animated welcome screen.
- Reworked mobile onboarding into a multi-step flow: status, personal details, identity evidence, and review.
- Added mobile identity document and selfie capture using Expo Camera on Android/iOS.
- Added the production-shaped KYC evidence MVP:
  - `POST /api/v1/kyc/me/applications`
  - `POST /api/v1/kyc/me/applications/{applicationId}/documents/upload-sessions`
  - signed `PUT` upload directly to object storage
  - `POST /api/v1/kyc/me/applications/{applicationId}/documents/{documentId}/confirm-upload`
  - `POST /api/v1/kyc/me/applications/{applicationId}/submissions`
- Added MinIO-backed signed upload/review URL storage adapter in KYC Service.
- KYC Service verifies uploaded objects exist before marking evidence uploaded.
- Final KYC review submission now requires uploaded `IDENTITY_DOCUMENT` and `SELFIE` evidence.
- Added admin evidence review:
  - `GET /api/v1/kyc/admin/applications/{applicationId}/evidence`
  - `POST /api/v1/kyc/admin/applications/{applicationId}/evidence/{documentId}/review-url`
- Added the document/liveness production plan in `docs/agent-notes/kyc-document-liveness-production-plan.md`.
- Added ADR `docs/decisions/0005-customer-kyc-evidence-mvp.md`.
- Added KYC admin review endpoints:
  - `GET /api/v1/kyc/admin/applications?status=PENDING_REVIEW`
  - `POST /api/v1/kyc/admin/applications/{applicationId}/decisions`
- Added append-only `kyc_decisions` records for admin decisions.
- Added User Service lifecycle sync endpoint:
  - `POST /api/v1/users/internal/customer-status`
- Wired the admin dashboard to the real KYC review queue and decision API.
- Added a mobile test harness path for authenticated KYC submission through the same gateway endpoints.
- Added Playwright onboarding automation and GitHub Actions workflow.

## What To Test

Run from the repository root:

```powershell
mvn test
npm test
npm run build --workspace admin/dashboard
npm run test:e2e --workspace mobile/app
npm run test:e2e --workspace admin/dashboard
```

The root browser automation command also works:

```powershell
npm run test:web
```

## Verification Performed

Automated checks run from the repository root:

```powershell
mvn test
npm test
npm run build --workspace admin/dashboard
npm run test:web
```

Docker-backed backend verification:

1. Started `infrastructure/docker-compose/compose.yml`.
2. Fixed the local MinIO image tag to `minio/minio:latest` because the previous `RELEASE.2026-03-13T19-27-58Z` tag does not exist.
3. Started User Service, KYC Service, and Gateway against Docker Keycloak and MinIO.
4. Retrieved real Keycloak tokens for `customer@finflow.local` and `admin@finflow.local`.
5. Called the real gateway endpoints:
   - `GET /api/v1/users/me`
   - `GET /api/v1/kyc/me`
   - `POST /api/v1/kyc/me/applications`
   - `POST /api/v1/kyc/me/applications/{applicationId}/documents/upload-sessions`
   - signed `PUT` upload to MinIO for identity evidence
   - `POST /api/v1/kyc/me/applications/{applicationId}/documents/{documentId}/confirm-upload`
   - signed `PUT` upload to MinIO for selfie evidence
   - `POST /api/v1/kyc/me/applications/{applicationId}/documents/{documentId}/confirm-upload`
   - `POST /api/v1/kyc/me/applications/{applicationId}/submissions`
   - `GET /api/v1/kyc/admin/applications?status=PENDING_REVIEW`
   - `GET /api/v1/kyc/admin/applications/{applicationId}/evidence`
   - `POST /api/v1/kyc/admin/applications/{applicationId}/evidence/{documentId}/review-url`
   - signed `GET` review URL from MinIO
   - `POST /api/v1/kyc/admin/applications/{applicationId}/decisions`
   - `GET /api/v1/users/me`
6. Observed customer lifecycle `REGISTERED -> KYC_APPROVED`, KYC `DRAFT -> PENDING_REVIEW -> APPROVED`, evidence `IDENTITY_DOCUMENT:UPLOADED,SELFIE:UPLOADED`, and review URL object bytes returned from MinIO.

Android emulator verification:

1. Started AVD `rn_light` with `-memory 1024`.
2. Started Expo on Android with a real customer Keycloak token and `EXPO_PUBLIC_GATEWAY_URL=http://10.0.2.2:8080`.
3. Confirmed Expo Go loaded the app on `emulator-5554`.
4. Captured Android UI hierarchy showing `Customer: KYC_APPROVED`, `KYC: APPROVED`, and the KYC onboarding form.
5. Stopped Expo, Spring Boot processes, emulator, and Docker containers after verification to free memory.

Additional Android smoke verification after style changes:

1. Started AVD `rn_light` with `-memory 1024`.
2. Started Expo on Android without a test token.
3. Confirmed the Android UI hierarchy rendered the icon image, `FinFlow`, `Wallet onboarding`, `Log in`, and `Sign up`.
4. Stopped Expo and the emulator after verification.

## Local Flow

1. Customer signs in or uses the test-token harness.
2. Mobile app calls `GET /api/v1/users/me` to create/load the profile.
3. Mobile app calls `GET /api/v1/kyc/me` to show KYC state.
4. Customer saves KYC metadata to `POST /api/v1/kyc/me/applications`.
5. Mobile app creates signed upload sessions for identity document and selfie evidence.
6. Mobile app uploads evidence bytes directly to MinIO using the signed upload URLs.
7. Mobile app confirms each upload through KYC Service.
8. Mobile app submits the application for admin review.
9. Admin dashboard loads pending applications and evidence metadata from KYC Service.
10. Admin opens short-lived evidence review URLs when reviewing the application.
11. Admin approves, rejects, or requests resubmission.
12. KYC Service records the decision and calls User Service to update customer lifecycle status.

## Important Boundary

Wallet activation is intentionally not implemented in this slice. The app reports approved KYC as wallet-eligible, but actual wallet creation belongs to the future Wallet Service and Ledger Service implementation.
