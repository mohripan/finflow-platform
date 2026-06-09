import { expect, test } from "@playwright/test";

test("renders login and registration entry points", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByText("FinFlow")).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Create account" })).toBeVisible();
});
