import { expect, test } from "@playwright/test";

test("submits customer KYC onboarding metadata", async ({ page }) => {
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
        data: { status: "NOT_SUBMITTED" },
        meta: { correlationId: "playwright-correlation-id" }
      })
    });
  });
  await page.route("http://gateway.test/api/v1/kyc/me/submissions", async (route) => {
    await expect(route.request().postDataJSON()).toEqual({
      legalName: "Ayu Lestari",
      dateOfBirth: "1996-05-20",
      nationalIdentityNumber: "3173123456789000",
      phoneNumber: "+6281234567890",
      address: "Jakarta Selatan"
    });
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
  await page.getByLabel("Legal name").fill("Ayu Lestari");
  await page.getByLabel("Date of birth YYYY-MM-DD").fill("1996-05-20");
  await page.getByLabel("National identity number").fill("3173123456789000");
  await page.getByLabel("Phone number").fill("+6281234567890");
  await page.getByLabel("Address").fill("Jakarta Selatan");
  await page.getByRole("button", { name: "Submit KYC" }).click();
  await expect(page.getByText("KYC submitted for review.")).toBeVisible();
});
