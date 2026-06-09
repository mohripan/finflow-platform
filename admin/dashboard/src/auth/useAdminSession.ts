import { useEffect, useState } from "react";
import { keycloak } from "../keycloak";
import type { AuthState } from "../types";

const testAccessToken = import.meta.env.VITE_TEST_ACCESS_TOKEN as string | undefined;

export function useAdminSession() {
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [profileEmail, setProfileEmail] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [authError, setAuthError] = useState("");

  useEffect(() => {
    if (testAccessToken) {
      setAccessToken(testAccessToken);
      setProfileEmail("test-admin@finflow.local");
      setAuthState("authenticated");
      return;
    }
    keycloak
      .init({ onLoad: "check-sso", pkceMethod: "S256" })
      .then((authenticated: boolean) => {
        setAuthState(authenticated ? "authenticated" : "anonymous");
        setAccessToken(keycloak.token ?? "");
        setProfileEmail(keycloak.tokenParsed?.email as string || keycloak.tokenParsed?.preferred_username as string || "");
      })
      .catch((err: unknown) => {
        setAuthError(err instanceof Error ? err.message : "Unable to initialize Keycloak.");
        setAuthState("anonymous");
      });
  }, []);

  function signOut() {
    if (!testAccessToken) {
      keycloak.logout();
    }
  }

  return {
    accessToken,
    authError,
    authState,
    isTestSession: Boolean(testAccessToken),
    profileEmail,
    signIn: () => keycloak.login(),
    signOut
  };
}
