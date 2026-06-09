import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  webServer: {
    command: "npm run dev",
    url: "http://127.0.0.1:5173",
    reuseExistingServer: true,
    timeout: 120000,
    env: {
      VITE_TEST_ACCESS_TOKEN: "playwright-admin-token",
      VITE_GATEWAY_URL: "http://gateway.test"
    }
  },
  use: {
    baseURL: "http://127.0.0.1:5173",
    ...devices["Desktop Chrome"]
  }
});
