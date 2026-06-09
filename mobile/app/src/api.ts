import * as Crypto from "expo-crypto";
import { gateway } from "./config";
import type { ApiProfile, KycForm, KycState } from "./types";

async function request<T>(path: string, accessToken: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${gateway}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Correlation-Id": Crypto.randomUUID(),
      ...(init?.headers ?? {})
    }
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.error?.message ?? `HTTP ${response.status}`);
  }
  return body.data;
}

export async function loadCustomerOnboarding(accessToken: string) {
  const [profile, kyc] = await Promise.all([
    request<ApiProfile>("/api/v1/users/me", accessToken),
    request<KycState>("/api/v1/kyc/me", accessToken)
  ]);
  return { profile, kyc };
}

export function submitKyc(accessToken: string, form: KycForm) {
  return request<KycState>("/api/v1/kyc/me/submissions", accessToken, {
    method: "POST",
    body: JSON.stringify(form)
  });
}
