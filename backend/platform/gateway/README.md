# Gateway

Spring Cloud Gateway entry point for public API routes.

Implemented slice:

- Validates bearer JWTs issued by Keycloak.
- Routes `/api/v1/users/**` to User Service.
- Routes `/api/v1/kyc/**` to KYC Service.
- Creates and propagates `X-Correlation-Id`.

Verification:

```text
mvn -pl backend/platform/gateway test
```
