import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import RefreshIcon from "@mui/icons-material/Refresh";
import ReplayIcon from "@mui/icons-material/Replay";
import {
  Alert,
  Box,
  Button,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography
} from "@mui/material";
import { useEffect, useState } from "react";
import { listPendingKycApplications, submitKycDecision } from "../api/kycReviewApi";
import type { KycApplication, KycDecision } from "../types";

type KycReviewScreenProps = {
  accessToken: string;
  profileEmail: string;
};

export function KycReviewScreen({ accessToken, profileEmail }: KycReviewScreenProps) {
  const [applications, setApplications] = useState<KycApplication[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [decision, setDecision] = useState<KycDecision>("APPROVE");
  const [reason, setReason] = useState("Identity details verified.");
  const [queueLoading, setQueueLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    loadQueue();
  }, [accessToken]);

  async function loadQueue() {
    if (!accessToken) return;
    setQueueLoading(true);
    setError("");
    try {
      const data = await listPendingKycApplications(accessToken);
      setApplications(data.applications);
      setSelectedId((current) => current || data.applications[0]?.applicationId || "");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load KYC review queue.");
    } finally {
      setQueueLoading(false);
    }
  }

  async function submitDecision() {
    if (!selectedId) return;
    setQueueLoading(true);
    setMessage("");
    setError("");
    try {
      const updated = await submitKycDecision(accessToken, selectedId, decision, reason);
      setMessage(`${updated.legalName} moved to ${updated.status}.`);
      setApplications((items) => items.filter((item) => item.applicationId !== selectedId));
      setSelectedId("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to submit KYC decision.");
    } finally {
      setQueueLoading(false);
    }
  }

  return (
    <Stack gap={3}>
      <Stack direction={{ xs: "column", sm: "row" }} gap={2} alignItems={{ sm: "center" }} justifyContent="space-between">
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 800 }}>Customer KYC review</Typography>
          <Typography color="text.secondary">Authenticated as {profileEmail || "admin user"}</Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={loadQueue} variant="outlined">Refresh queue</Button>
      </Stack>
      {message ? <Alert severity="success">{message}</Alert> : null}
      {error ? <Alert severity="warning">{error}</Alert> : null}
      <KycApplicationsTable
        applications={applications}
        queueLoading={queueLoading}
        selectedId={selectedId}
        onSelect={setSelectedId}
      />
      <Stack direction={{ xs: "column", md: "row" }} gap={2} alignItems={{ md: "center" }}>
        <TextField select label="Decision" value={decision} onChange={(event) => setDecision(event.target.value as KycDecision)} sx={{ minWidth: 220 }}>
          <MenuItem value="APPROVE">Approve</MenuItem>
          <MenuItem value="REJECT">Reject</MenuItem>
          <MenuItem value="REQUEST_RESUBMISSION">Request resubmission</MenuItem>
        </TextField>
        <TextField label="Decision reason" value={reason} onChange={(event) => setReason(event.target.value)} fullWidth />
        <Button
          startIcon={decision === "APPROVE" ? <CheckCircleIcon /> : <ReplayIcon />}
          onClick={submitDecision}
          disabled={!selectedId || queueLoading}
          variant="contained"
          sx={{ minHeight: 56, minWidth: 170 }}
        >
          Submit decision
        </Button>
      </Stack>
    </Stack>
  );
}

function KycApplicationsTable(props: {
  applications: KycApplication[];
  queueLoading: boolean;
  selectedId: string;
  onSelect: (applicationId: string) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
      <Table aria-label="KYC review queue">
        <TableHead>
          <TableRow>
            <TableCell>Applicant</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Phone</TableCell>
            <TableCell>Address</TableCell>
            <TableCell align="right">Rejections</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {props.applications.map((application) => (
            <TableRow
              hover
              selected={application.applicationId === props.selectedId}
              key={application.applicationId}
              onClick={() => props.onSelect(application.applicationId)}
              sx={{ cursor: "pointer" }}
            >
              <TableCell>
                <Typography fontWeight={700}>{application.legalName}</Typography>
                <Typography variant="body2" color="text.secondary">{application.applicationId}</Typography>
              </TableCell>
              <TableCell>{application.status}</TableCell>
              <TableCell>{application.phoneNumber}</TableCell>
              <TableCell>{application.address}</TableCell>
              <TableCell align="right">{application.rejectionCount}</TableCell>
            </TableRow>
          ))}
          {!props.queueLoading && props.applications.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5}>
                <Typography color="text.secondary">No pending KYC applications.</Typography>
              </TableCell>
            </TableRow>
          ) : null}
        </TableBody>
      </Table>
    </Paper>
  );
}
