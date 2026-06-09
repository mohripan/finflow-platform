import { expect, test } from "@playwright/test";

test("renders admin login shell", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByText("FinFlow Admin")).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign in with Keycloak" })).toBeVisible();
});
