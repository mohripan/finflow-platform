import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  webServer: {
    command: "npm run web",
    url: "http://localhost:19006",
    reuseExistingServer: true,
    timeout: 120000,
    env: {
      EXPO_PUBLIC_TEST_ACCESS_TOKEN: "playwright-customer-token",
      EXPO_PUBLIC_GATEWAY_URL: "http://gateway.test"
    }
  },
  use: {
    baseURL: "http://localhost:19006",
    ...devices["Desktop Chrome"]
  }
});
