import { expect, test } from "@playwright/test";

test("reviews and approves a pending customer KYC application", async ({ page }) => {
  await page.route("http://gateway.test/api/v1/kyc/admin/applications?status=PENDING_REVIEW", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          applications: [{
            applicationId: "kyc_e2e_customer",
            status: "PENDING_REVIEW",
            legalName: "Ayu Lestari",
            dateOfBirth: "1996-05-20",
            phoneNumber: "+6281234567890",
            address: "Jakarta Selatan",
            rejectionCount: 0
          }]
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/admin/applications/kyc_e2e_customer/evidence", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
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
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/admin/applications/kyc_e2e_customer/evidence/doc_identity/review-url", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          documentId: "doc_identity",
          reviewUrl: "http://storage.test/review/doc_identity",
          expiresAt: "2026-06-09T00:15:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/admin/applications/kyc_e2e_customer/decisions", async (route) => {
    await expect(route.request().postDataJSON()).toEqual({
      decision: "APPROVE",
      reason: "Identity details verified."
    });
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          applicationId: "kyc_e2e_customer",
          status: "APPROVED",
          legalName: "Ayu Lestari",
          dateOfBirth: "1996-05-20",
          phoneNumber: "+6281234567890",
          address: "Jakarta Selatan",
          rejectionCount: 0,
          reviewedBy: "admin-subject"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });

  await page.goto("/");
  await expect(page.getByText("FinFlow Admin")).toBeVisible();
  await expect(page.getByText("Ayu Lestari")).toBeVisible();
  await expect(page.getByText("KTP or SIM")).toBeVisible();
  await page.getByRole("button", { name: "Open evidence" }).first().click();
  await page.getByRole("button", { name: "Submit decision" }).click();
  await expect(page.getByText("Ayu Lestari moved to APPROVED.")).toBeVisible();
});
