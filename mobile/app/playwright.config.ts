import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  webServer: {
    command: "npm run web",
    url: "http://localhost:19006",
    reuseExistingServer: true,
    timeout: 120000
  },
  use: {
    baseURL: "http://localhost:19006",
    ...devices["Desktop Chrome"]
  }
});
