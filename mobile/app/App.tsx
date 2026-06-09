import * as AuthSession from "expo-auth-session";
import React, { useMemo, useState } from "react";
import { loadCustomerOnboarding, submitKyc as submitKycRequest } from "./src/api";
import { clientId, issuer, testAccessToken } from "./src/config";
import { LoginScreen } from "./src/screens/LoginScreen";
import { OnboardingScreen } from "./src/screens/OnboardingScreen";
import type { ApiProfile, KycState } from "./src/types";
import { emptyKycForm } from "./src/types";

export default function App() {
  const redirectUri = AuthSession.makeRedirectUri({ scheme: "finflow", path: "auth" });
  const discovery = AuthSession.useAutoDiscovery(issuer);
  const [token, setToken] = useState<string | null>(null);
  const [profile, setProfile] = useState<ApiProfile | null>(null);
  const [kyc, setKyc] = useState<KycState | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(emptyKycForm);

  React.useEffect(() => {
    if (!testAccessToken || token) return;
    setToken(testAccessToken);
    refresh(testAccessToken);
  }, [token]);

  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId,
      redirectUri,
      responseType: AuthSession.ResponseType.Code,
      scopes: ["openid", "profile", "email"],
      usePKCE: true
    },
    discovery
  );

  React.useEffect(() => {
    async function finishAuth() {
      if (response?.type !== "success" || !discovery || !request?.codeVerifier) return;
      setLoading(true);
      try {
        const result = await AuthSession.exchangeCodeAsync(
          {
            clientId,
            code: response.params.code,
            redirectUri,
            extraParams: { code_verifier: request.codeVerifier }
          },
          discovery
        );
        setToken(result.accessToken);
        await refresh(result.accessToken);
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "Authentication failed.");
      } finally {
        setLoading(false);
      }
    }
    finishAuth();
  }, [response, discovery, request?.codeVerifier, redirectUri]);

  const registrationUrl = useMemo(() => {
    const params = new URLSearchParams({
      client_id: clientId,
      response_type: "code",
      scope: "openid profile email",
      redirect_uri: redirectUri
    });
    return `${issuer}/protocol/openid-connect/registrations?${params.toString()}`;
  }, [redirectUri]);

  async function refresh(accessToken = token) {
    if (!accessToken) return;
    const next = await loadCustomerOnboarding(accessToken);
    setProfile(next.profile);
    setKyc(next.kyc);
  }

  async function submitKyc() {
    if (!token) return;
    setLoading(true);
    setMessage("");
    try {
      setKyc(await submitKycRequest(token, form));
      await refresh(token);
      setMessage("KYC submitted for review.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "KYC submission failed.");
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <LoginScreen
        registrationUrl={registrationUrl}
        canSignIn={Boolean(request && discovery)}
        onSignIn={() => promptAsync()}
      />
    );
  }

  return (
    <OnboardingScreen
      profile={profile}
      kyc={kyc}
      form={form}
      loading={loading}
      message={message}
      onChangeForm={setForm}
      onSubmit={submitKyc}
    />
  );
}
