import { CircularProgress, Stack, Typography } from "@mui/material";

export function LoadingPanel() {
  return (
    <Stack alignItems="center" gap={2} sx={{ py: 8 }}>
      <CircularProgress />
      <Typography>Checking admin session</Typography>
    </Stack>
  );
}
