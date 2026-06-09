import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import LoginIcon from "@mui/icons-material/Login";
import LogoutIcon from "@mui/icons-material/Logout";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Alert, AppBar, Box, Button, CircularProgress, Container, Stack, Toolbar, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { keycloak } from "./keycloak";

type AuthState = "loading" | "anonymous" | "authenticated" | "error";

export default function App() {
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [profileEmail, setProfileEmail] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    keycloak
      .init({ onLoad: "check-sso", pkceMethod: "S256" })
      .then((authenticated) => {
        setAuthState(authenticated ? "authenticated" : "anonymous");
        setProfileEmail(keycloak.tokenParsed?.email as string || keycloak.tokenParsed?.preferred_username as string || "");
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to initialize Keycloak.");
        setAuthState("anonymous");
      });
  }, []);

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "#f6f7f8" }}>
      <AppBar position="static" color="inherit" elevation={0} sx={{ borderBottom: "1px solid #dfe4e8" }}>
        <Toolbar>
          <AdminPanelSettingsIcon sx={{ mr: 1, color: "#0f6b57" }} />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 800 }}>FinFlow Admin</Typography>
          {authState === "authenticated" ? (
            <Button startIcon={<LogoutIcon />} onClick={() => keycloak.logout()} variant="outlined">Sign out</Button>
          ) : null}
        </Toolbar>
      </AppBar>
      <Container maxWidth="md" sx={{ py: 5 }}>
        {authState === "loading" ? (
          <Stack alignItems="center" gap={2} sx={{ py: 8 }}>
            <CircularProgress />
            <Typography>Checking admin session</Typography>
          </Stack>
        ) : null}
        {authState === "anonymous" ? (
          <Stack gap={3}>
            {error ? <Alert severity="warning">{error}</Alert> : null}
            <Typography variant="h3" sx={{ fontWeight: 800 }}>Operations login</Typography>
            <Typography color="text.secondary">
              Admin workflows require a Keycloak admin, compliance, or support session. Review queues are not shown until the backend admin APIs exist.
            </Typography>
            <Box>
              <Button startIcon={<LoginIcon />} onClick={() => keycloak.login()} variant="contained" size="large">Sign in with Keycloak</Button>
            </Box>
          </Stack>
        ) : null}
        {authState === "authenticated" ? (
          <Stack gap={3}>
            <Typography variant="h4" sx={{ fontWeight: 800 }}>Signed in</Typography>
            <Alert severity="info">Authenticated as {profileEmail || "admin user"}. KYC review queues will connect to `/api/v1/kyc/admin/applications` when that service slice is implemented.</Alert>
            <Box>
              <Button startIcon={<RefreshIcon />} onClick={() => keycloak.updateToken(30)} variant="outlined">Refresh token</Button>
            </Box>
          </Stack>
        ) : null}
      </Container>
    </Box>
  );
}
