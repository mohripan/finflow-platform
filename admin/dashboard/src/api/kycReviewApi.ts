import type { KycApplication, KycDecision, KycDocument } from "../types";

const gatewayUrl = import.meta.env.VITE_GATEWAY_URL ?? "http://localhost:8080";

async function request<T>(accessToken: string, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${gatewayUrl}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Correlation-Id": crypto.randomUUID(),
      ...(init?.headers ?? {})
    }
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.error?.message ?? `HTTP ${response.status}`);
  }
  return body.data;
}

export function listPendingKycApplications(accessToken: string) {
  return request<{ applications: KycApplication[] }>(
    accessToken,
    "/api/v1/kyc/admin/applications?status=PENDING_REVIEW"
  );
}

export function submitKycDecision(accessToken: string, applicationId: string, decision: KycDecision, reason: string) {
  return request<KycApplication>(accessToken, `/api/v1/kyc/admin/applications/${applicationId}/decisions`, {
    method: "POST",
    body: JSON.stringify({ decision, reason })
  });
}

export function listKycEvidence(accessToken: string, applicationId: string) {
  return request<{ documents: KycDocument[] }>(accessToken, `/api/v1/kyc/admin/applications/${applicationId}/evidence`);
}

export function createKycEvidenceReviewUrl(accessToken: string, applicationId: string, documentId: string) {
  return request<{ documentId: string; reviewUrl: string; expiresAt: string }>(
    accessToken,
    `/api/v1/kyc/admin/applications/${applicationId}/evidence/${documentId}/review-url`,
    { method: "POST" }
  );
}
