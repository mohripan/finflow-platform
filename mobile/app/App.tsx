import { StatusBar } from "expo-status-bar";
import * as AuthSession from "expo-auth-session";
import * as Crypto from "expo-crypto";
import * as Linking from "expo-linking";
import React, { useMemo, useState } from "react";
import { ActivityIndicator, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

type ApiProfile = {
  userId: string;
  email: string;
  displayName?: string;
  status: string;
  customerAccount: { customerId: string; status: string };
};

type KycState = {
  status: string;
  legalName?: string;
  phoneNumber?: string;
};

const issuer = process.env.EXPO_PUBLIC_KEYCLOAK_ISSUER ?? "http://localhost:8180/realms/finflow-local";
const gateway = process.env.EXPO_PUBLIC_GATEWAY_URL ?? "http://localhost:8080";
const clientId = process.env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID ?? "finflow-mobile";

export default function App() {
  const redirectUri = AuthSession.makeRedirectUri({ scheme: "finflow", path: "auth" });
  const discovery = AuthSession.useAutoDiscovery(issuer);
  const [token, setToken] = useState<string | null>(null);
  const [profile, setProfile] = useState<ApiProfile | null>(null);
  const [kyc, setKyc] = useState<KycState | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    legalName: "",
    dateOfBirth: "",
    nationalIdentityNumber: "",
    phoneNumber: "",
    address: ""
  });

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

  async function api<T>(path: string, accessToken: string, init?: RequestInit): Promise<T> {
    const res = await fetch(`${gateway}${path}`, {
      ...init,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
        "X-Correlation-Id": Crypto.randomUUID(),
        ...(init?.headers ?? {})
      }
    });
    const body = await res.json();
    if (!res.ok) throw new Error(body.error?.message ?? `HTTP ${res.status}`);
    return body.data;
  }

  async function refresh(accessToken = token) {
    if (!accessToken) return;
    const nextProfile = await api<ApiProfile>("/api/v1/users/me", accessToken);
    const nextKyc = await api<KycState>("/api/v1/kyc/me", accessToken);
    setProfile(nextProfile);
    setKyc(nextKyc);
  }

  async function submitKyc() {
    if (!token) return;
    setLoading(true);
    setMessage("");
    try {
      const payload = { ...form };
      const submitted = await api<KycState>("/api/v1/kyc/me/submissions", token, {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setKyc(submitted);
      setMessage("KYC submitted for review.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "KYC submission failed.");
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <SafeAreaView style={styles.screen}>
        <StatusBar style="dark" />
        <View style={styles.hero}>
          <Text style={styles.brand}>FinFlow</Text>
          <Text style={styles.title}>Wallet onboarding</Text>
          <Text style={styles.copy}>Sign in or create a Keycloak account before submitting customer KYC.</Text>
        </View>
        <View style={styles.actions}>
          <Pressable accessibilityRole="button" disabled={!request || !discovery} onPress={() => promptAsync()} style={styles.primary}>
            <Text style={styles.primaryText}>Sign in</Text>
          </Pressable>
          <Pressable accessibilityRole="button" onPress={() => Linking.openURL(registrationUrl)} style={styles.secondary}>
            <Text style={styles.secondaryText}>Create account</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.brand}>FinFlow</Text>
        <View style={styles.panel}>
          <Text style={styles.sectionTitle}>{profile?.displayName || profile?.email || "Profile"}</Text>
          <Text style={styles.muted}>Customer: {profile?.customerAccount.status ?? "loading"}</Text>
          <Text style={styles.muted}>KYC: {kyc?.status ?? "loading"}</Text>
        </View>
        <View style={styles.panel}>
          <Text style={styles.sectionTitle}>KYC onboarding</Text>
          <Field label="Legal name" value={form.legalName} onChangeText={(legalName) => setForm({ ...form, legalName })} />
          <Field label="Date of birth YYYY-MM-DD" value={form.dateOfBirth} onChangeText={(dateOfBirth) => setForm({ ...form, dateOfBirth })} />
          <Field label="National identity number" value={form.nationalIdentityNumber} onChangeText={(nationalIdentityNumber) => setForm({ ...form, nationalIdentityNumber })} />
          <Field label="Phone number" value={form.phoneNumber} onChangeText={(phoneNumber) => setForm({ ...form, phoneNumber })} />
          <Field label="Address" value={form.address} onChangeText={(address) => setForm({ ...form, address })} multiline />
          <Pressable accessibilityRole="button" onPress={submitKyc} disabled={loading} style={styles.primary}>
            <Text style={styles.primaryText}>{loading ? "Submitting..." : "Submit KYC"}</Text>
          </Pressable>
          {loading ? <ActivityIndicator /> : null}
          {message ? <Text style={styles.message}>{message}</Text> : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function Field(props: { label: string; value: string; onChangeText: (value: string) => void; multiline?: boolean }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{props.label}</Text>
      <TextInput style={[styles.input, props.multiline ? styles.multi : null]} value={props.value} onChangeText={props.onChangeText} multiline={props.multiline} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#f7f8f5" },
  content: { padding: 20, gap: 16 },
  hero: { padding: 28, gap: 10 },
  brand: { fontSize: 20, fontWeight: "800", color: "#0f3d36" },
  title: { fontSize: 34, fontWeight: "800", color: "#17201d" },
  copy: { fontSize: 16, lineHeight: 24, color: "#52605a" },
  actions: { padding: 24, gap: 12 },
  panel: { backgroundColor: "#ffffff", borderRadius: 8, padding: 16, gap: 12, borderWidth: 1, borderColor: "#dce2de" },
  sectionTitle: { fontSize: 18, fontWeight: "700", color: "#17201d" },
  muted: { color: "#52605a" },
  field: { gap: 6 },
  label: { color: "#2f3d38", fontWeight: "600" },
  input: { borderWidth: 1, borderColor: "#c9d2cd", borderRadius: 6, paddingHorizontal: 12, paddingVertical: 10, backgroundColor: "#fff" },
  multi: { minHeight: 76, textAlignVertical: "top" },
  primary: { minHeight: 46, borderRadius: 6, backgroundColor: "#0f6b57", alignItems: "center", justifyContent: "center", paddingHorizontal: 16 },
  primaryText: { color: "#fff", fontWeight: "800" },
  secondary: { minHeight: 46, borderRadius: 6, borderWidth: 1, borderColor: "#0f6b57", alignItems: "center", justifyContent: "center", paddingHorizontal: 16 },
  secondaryText: { color: "#0f6b57", fontWeight: "800" },
  message: { color: "#0f3d36", fontWeight: "600" }
});
