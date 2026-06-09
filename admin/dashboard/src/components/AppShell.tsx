import LogoutIcon from "@mui/icons-material/Logout";
import { AppBar, Box, Button, Container, Toolbar, Typography } from "@mui/material";
import type { ReactNode } from "react";
import icon from "../assets/images/icon.png";
import type { AuthState } from "../types";

type AppShellProps = {
  authState: AuthState;
  children: ReactNode;
  onSignOut: () => void;
};

export function AppShell({ authState, children, onSignOut }: AppShellProps) {
  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "#f4f8ff" }}>
      <AppBar position="static" color="inherit" elevation={0} sx={{ borderBottom: "1px solid #d7e7ff" }}>
        <Toolbar>
          <Box component="img" src={icon} alt="" sx={{ width: 32, height: 32, borderRadius: 1, mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 800 }}>FinFlow Admin</Typography>
          {authState === "authenticated" ? (
            <Button startIcon={<LogoutIcon />} onClick={onSignOut} variant="outlined">Sign out</Button>
          ) : null}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>{children}</Container>
    </Box>
  );
}
