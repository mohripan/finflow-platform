import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "../src/App";

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
  });

  it("shows the Keycloak admin login entry point", async () => {
    render(<App />);
    expect(await screen.findByRole("button", { name: /sign in with keycloak/i })).toBeInTheDocument();
  });
});
