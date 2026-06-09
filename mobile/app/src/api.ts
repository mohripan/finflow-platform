import * as Crypto from "expo-crypto";
import { gateway } from "./config";
import type { ApiProfile, CapturedEvidence, KycDocumentUploadSession, KycForm, KycState } from "./types";

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

export function createKycDraft(accessToken: string, form: KycForm) {
  return request<KycState>("/api/v1/kyc/me/applications", accessToken, {
    method: "POST",
    body: JSON.stringify(form)
  });
}

export async function uploadKycEvidence(accessToken: string, applicationId: string, evidence: CapturedEvidence) {
  const session = await request<KycDocumentUploadSession>(
    `/api/v1/kyc/me/applications/${applicationId}/documents/upload-sessions`,
    accessToken,
    {
      method: "POST",
      body: JSON.stringify({
        documentType: evidence.documentType,
        contentType: evidence.contentType,
        sizeBytes: evidence.sizeBytes,
        checksum: evidence.checksum
      })
    }
  );
  const objectResponse = await fetch(session.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": evidence.contentType },
    body: await fetch(evidence.uri).then((response) => response.blob())
  });
  if (!objectResponse.ok) {
    throw new Error(`Document upload failed with HTTP ${objectResponse.status}`);
  }
  return request(
    `/api/v1/kyc/me/applications/${applicationId}/documents/${session.documentId}/confirm-upload`,
    accessToken,
    {
      method: "POST",
      body: JSON.stringify({ checksum: evidence.checksum })
    }
  );
}

export function submitKycForReview(accessToken: string, applicationId: string) {
  return request<KycState>(`/api/v1/kyc/me/applications/${applicationId}/submissions`, accessToken, {
    method: "POST"
  });
}
