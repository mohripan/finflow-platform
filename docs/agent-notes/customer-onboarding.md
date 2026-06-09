# Customer Onboarding Agent Notes

## What Changed

- Refactored KYC Service into `api`, `application`, `domain`, and `infrastructure` packages.
- Refactored User Service into `api`, `application`, `domain`, and `infrastructure` packages.
- Added use-case interfaces for KYC onboarding and user profile lifecycle operations.
- Split the mobile app into API, config, type, component, style, and screen modules under `mobile/app/src`.
- Split the admin dashboard into API, auth, type, component, and screen modules under `admin/dashboard/src`.
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
3. Started User Service, KYC Service, and Gateway against Docker PostgreSQL and Keycloak.
4. Retrieved real Keycloak tokens for `customer@finflow.local` and `admin@finflow.local`.
5. Called the real gateway endpoints:
   - `GET /api/v1/users/me`
   - `GET /api/v1/kyc/me`
   - `POST /api/v1/kyc/me/submissions`
   - `GET /api/v1/kyc/admin/applications?status=PENDING_REVIEW`
   - `POST /api/v1/kyc/admin/applications/{applicationId}/decisions`
   - `GET /api/v1/users/me`
6. Observed customer lifecycle `REGISTERED -> KYC_APPROVED` and KYC `NOT_SUBMITTED -> PENDING_REVIEW -> APPROVED`.

Android emulator verification:

1. Started AVD `rn_light` with `-memory 1024`.
2. Started Expo on Android with a real customer Keycloak token and `EXPO_PUBLIC_GATEWAY_URL=http://10.0.2.2:8080`.
3. Confirmed Expo Go loaded the app on `emulator-5554`.
4. Captured Android UI hierarchy showing `Customer: KYC_APPROVED`, `KYC: APPROVED`, and the KYC onboarding form.
5. Stopped Expo, Spring Boot processes, emulator, and Docker containers after verification to free memory.

## Local Flow

1. Customer signs in or uses the test-token harness.
2. Mobile app calls `GET /api/v1/users/me` to create/load the profile.
3. Mobile app calls `GET /api/v1/kyc/me` to show KYC state.
4. Customer submits KYC metadata to `POST /api/v1/kyc/me/submissions`.
5. Admin dashboard loads pending applications from KYC Service.
6. Admin approves, rejects, or requests resubmission.
7. KYC Service records the decision and calls User Service to update customer lifecycle status.

## Important Boundary

Wallet activation is intentionally not implemented in this slice. The app reports approved KYC as wallet-eligible, but actual wallet creation belongs to the future Wallet Service and Ledger Service implementation.
