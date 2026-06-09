export const issuer = process.env.EXPO_PUBLIC_KEYCLOAK_ISSUER ?? "http://localhost:8180/realms/finflow-local";
export const gateway = process.env.EXPO_PUBLIC_GATEWAY_URL ?? "http://localhost:8080";
export const clientId = process.env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID ?? "finflow-mobile";
export const testAccessToken = process.env.EXPO_PUBLIC_TEST_ACCESS_TOKEN;
