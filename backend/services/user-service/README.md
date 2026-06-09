# User Service

Owns FinFlow application profiles and the customer account lifecycle mapping to a Keycloak subject.

Implemented slice:

- `GET /api/v1/users/me` creates or loads a profile idempotently from the authenticated JWT subject.
- `PATCH /api/v1/users/me` updates non-sensitive profile preferences.
- Credentials remain in Keycloak. This service never stores passwords.

Verification:

```text
mvn -pl backend/services/user-service test
```
