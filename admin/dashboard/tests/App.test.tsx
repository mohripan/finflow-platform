import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "../src/App";
import { keycloak } from "../src/keycloak";

vi.mock("../src/keycloak", () => ({
  keycloak: {
    init: vi.fn(() => Promise.resolve(false)),
    login: vi.fn(),
    logout: vi.fn(),
    updateToken: vi.fn(),
    tokenParsed: {}
  }
}));

describe("App", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("fetch", vi.fn());
    Object.defineProperty(globalThis, "crypto", {
      value: { randomUUID: () => "test-correlation-id" },
      configurable: true
    });
  });

  it("shows the Keycloak admin login entry point", async () => {
    render(<App />);
    expect(await screen.findByRole("button", { name: /sign in with keycloak/i })).toBeInTheDocument();
  });

  it("loads pending KYC applications and submits an approval decision", async () => {
    vi.mocked(keycloak.init).mockResolvedValue(true);
    Object.assign(keycloak, {
      token: "admin-token",
      tokenParsed: { email: "admin@example.test" }
    });
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify({
        data: {
          applications: [{
            applicationId: "kyc_123",
            status: "PENDING_REVIEW",
            legalName: "Ayu Lestari",
            dateOfBirth: "1996-05-20",
            phoneNumber: "+6281234567890",
            address: "Jakarta Selatan",
            rejectionCount: 0
          }]
        },
        meta: { correlationId: "test-correlation-id" }
      }), { status: 200, headers: { "Content-Type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        data: {
          documents: [{
            documentId: "doc_identity",
            documentType: "IDENTITY_DOCUMENT",
            status: "UPLOADED",
            contentType: "image/jpeg",
            sizeBytes: 2048,
            checksum: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            createdAt: "2026-06-09T00:00:00Z",
            updatedAt: "2026-06-09T00:00:00Z"
          }, {
            documentId: "doc_selfie",
            documentType: "SELFIE",
            status: "UPLOADED",
            contentType: "image/jpeg",
            sizeBytes: 1024,
            checksum: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            createdAt: "2026-06-09T00:00:00Z",
            updatedAt: "2026-06-09T00:00:00Z"
          }]
        },
        meta: { correlationId: "test-correlation-id" }
      }), { status: 200, headers: { "Content-Type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        data: {
          applicationId: "kyc_123",
          status: "APPROVED",
          legalName: "Ayu Lestari",
          dateOfBirth: "1996-05-20",
          phoneNumber: "+6281234567890",
          address: "Jakarta Selatan",
          rejectionCount: 0,
          reviewedBy: "admin-subject"
        },
        meta: { correlationId: "test-correlation-id" }
      }), { status: 200, headers: { "Content-Type": "application/json" } }));

    render(<App />);

    expect(await screen.findByText("Ayu Lestari")).toBeInTheDocument();
    expect(await screen.findByText("KTP or SIM")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /submit decision/i }));

    await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(
      "http://localhost:8080/api/v1/kyc/admin/applications/kyc_123/decisions",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ decision: "APPROVE", reason: "Identity details verified." })
      })
    ));
    expect(await screen.findByText("Ayu Lestari moved to APPROVED.")).toBeInTheDocument();
  });
});
