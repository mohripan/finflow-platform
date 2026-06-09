import { expect, test } from "@playwright/test";

test("submits customer KYC onboarding with signed evidence uploads", async ({ page }) => {
  let submitted = false;
  await page.route("http://gateway.test/api/v1/users/me", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          userId: "usr_e2e_customer",
          email: "ayu@example.test",
          displayName: "Ayu",
          status: "ACTIVE",
          customerAccount: { customerId: "cus_e2e_customer", status: "REGISTERED" },
          createdAt: "2026-06-09T00:00:00Z",
          updatedAt: "2026-06-09T00:00:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/me", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: submitted ? {
          applicationId: "kyc_e2e_customer",
          status: "PENDING_REVIEW",
          legalName: "Ayu Lestari",
          phoneNumber: "+6281234567890"
        } : { status: "NOT_SUBMITTED" },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/me/applications", async (route) => {
    await expect(route.request().postDataJSON()).toEqual({
      legalName: "Ayu Lestari",
      dateOfBirth: "1996-05-20",
      nationalIdentityNumber: "3173123456789000",
      phoneNumber: "+6281234567890",
      address: "Jakarta Selatan"
    });
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          applicationId: "kyc_e2e_customer",
          status: "DRAFT",
          legalName: "Ayu Lestari",
          dateOfBirth: "1996-05-20",
          phoneNumber: "+6281234567890",
          address: "Jakarta Selatan",
          rejectionCount: 0,
          createdAt: "2026-06-09T00:00:00Z",
          updatedAt: "2026-06-09T00:00:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/me/applications/kyc_e2e_customer/documents/upload-sessions", async (route) => {
    const body = route.request().postDataJSON();
    expect(["IDENTITY_DOCUMENT", "SELFIE"]).toContain(body.documentType);
    expect(body.contentType).toBe("image/jpeg");
    expect(body.sizeBytes).toBeGreaterThan(0);
    expect(body.checksum).toMatch(/^[a-f0-9]{64}$/);
    const suffix = body.documentType === "IDENTITY_DOCUMENT" ? "identity" : "selfie";
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          documentId: `doc_${suffix}`,
          documentType: body.documentType,
          uploadUrl: `http://storage.test/upload/${suffix}`,
          expiresAt: "2026-06-09T00:15:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://storage.test/upload/*", async (route) => {
    expect(route.request().method()).toBe("PUT");
    expect(route.request().headers()["content-type"]).toContain("image/jpeg");
    await route.fulfill({ status: 200, body: "" });
  });
  await page.route(/http:\/\/gateway\.test\/api\/v1\/kyc\/me\/applications\/kyc_e2e_customer\/documents\/doc_(identity|selfie)\/confirm-upload/, async (route) => {
    expect(route.request().postDataJSON().checksum).toMatch(/^[a-f0-9]{64}$/);
    const documentType = route.request().url().includes("doc_identity") ? "IDENTITY_DOCUMENT" : "SELFIE";
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          documentId: documentType === "IDENTITY_DOCUMENT" ? "doc_identity" : "doc_selfie",
          documentType,
          status: "UPLOADED",
          contentType: "image/jpeg",
          sizeBytes: 32,
          checksum: route.request().postDataJSON().checksum,
          createdAt: "2026-06-09T00:00:00Z",
          updatedAt: "2026-06-09T00:00:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/me/applications/kyc_e2e_customer/submissions", async (route) => {
    expect(route.request().method()).toBe("POST");
    submitted = true;
    await route.fulfill({
      status: 202,
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          applicationId: "kyc_e2e_customer",
          status: "PENDING_REVIEW",
          legalName: "Ayu Lestari",
          dateOfBirth: "1996-05-20",
          phoneNumber: "+6281234567890",
          address: "Jakarta Selatan",
          rejectionCount: 0,
          createdAt: "2026-06-09T00:00:00Z",
          updatedAt: "2026-06-09T00:00:00Z"
        },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });

  await page.goto("/");
  await expect(page.getByText("FinFlow")).toBeVisible();
  await expect(page.getByText("KYC: NOT_SUBMITTED")).toBeVisible();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByLabel("Legal name").fill("Ayu Lestari");
  await page.getByLabel("Date of birth YYYY-MM-DD").fill("1996-05-20");
  await page.getByLabel("National identity number").fill("3173123456789000");
  await page.getByLabel("Phone number").fill("+6281234567890");
  await page.getByLabel("Address").fill("Jakarta Selatan");
  await page.getByRole("button", { name: "Continue" }).click();
  await expect(page.getByText("Identity evidence")).toBeVisible();
  await page.getByRole("button", { name: "Capture" }).first().click();
  await expect(page.getByText(/KB ready/).first()).toBeVisible();
  await page.getByRole("button", { name: "Capture" }).click();
  await expect(page.getByText(/KB ready/).nth(1)).toBeVisible();
  await page.getByRole("button", { name: "Continue" }).click();
  await expect(page.getByText("Review submission")).toBeVisible();
  await page.getByRole("button", { name: "Submit KYC" }).click();
  await expect(page.getByText("KYC submitted for review.")).toBeVisible();
});
