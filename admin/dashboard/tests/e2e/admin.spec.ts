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
  await page.getByRole("button", { name: "Submit decision" }).click();
  await expect(page.getByText("Ayu Lestari moved to APPROVED.")).toBeVisible();
});
