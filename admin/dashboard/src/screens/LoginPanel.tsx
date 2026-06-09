import LoginIcon from "@mui/icons-material/Login";
import { Alert, Box, Button, Stack, Typography } from "@mui/material";

type LoginPanelProps = {
  error: string;
  onSignIn: () => void;
};

export function LoginPanel({ error, onSignIn }: LoginPanelProps) {
  return (
    <Stack gap={3}>
      {error ? <Alert severity="warning">{error}</Alert> : null}
      <Typography variant="h3" sx={{ fontWeight: 800 }}>Operations login</Typography>
      <Typography color="text.secondary">Admin workflows require a Keycloak admin, compliance, or support session.</Typography>
      <Box>
        <Button startIcon={<LoginIcon />} onClick={onSignIn} variant="contained" size="large">Sign in with Keycloak</Button>
      </Box>
    </Stack>
  );
}
