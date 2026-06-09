# Admin Dashboard

React + Material UI admin shell for Keycloak-authenticated operations users.

Implemented slice:

- Keycloak admin login/logout.
- No fake KYC/KYB queues. Admin review screens should be added only after matching backend admin APIs and audit records exist.

Verification:

```text
npm run test --workspace admin/dashboard
npm run test:e2e --workspace admin/dashboard
```
