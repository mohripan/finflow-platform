import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import RefreshIcon from "@mui/icons-material/Refresh";
import ReplayIcon from "@mui/icons-material/Replay";
import {
  Alert,
  Box,
  Button,
  Chip,
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
import { createKycEvidenceReviewUrl, listKycEvidence, listPendingKycApplications, submitKycDecision } from "../api/kycReviewApi";
import type { KycApplication, KycDecision, KycDocument } from "../types";

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
  const [evidence, setEvidence] = useState<KycDocument[]>([]);
  const [evidenceLoading, setEvidenceLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    loadQueue();
  }, [accessToken]);

  useEffect(() => {
    if (!selectedId) {
      setEvidence([]);
      return;
    }
    loadEvidence(selectedId);
  }, [accessToken, selectedId]);

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

  async function loadEvidence(applicationId: string) {
    setEvidenceLoading(true);
    setError("");
    try {
      const data = await listKycEvidence(accessToken, applicationId);
      setEvidence(data.documents);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load KYC evidence.");
    } finally {
      setEvidenceLoading(false);
    }
  }

  async function openEvidence(documentId: string) {
    if (!selectedId) return;
    setError("");
    try {
      const data = await createKycEvidenceReviewUrl(accessToken, selectedId, documentId);
      window.open(data.reviewUrl, "_blank", "noopener,noreferrer");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to create KYC evidence review URL.");
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
      <EvidenceTable evidence={evidence} loading={evidenceLoading} onOpen={openEvidence} />
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

function EvidenceTable({ evidence, loading, onOpen }: { evidence: KycDocument[]; loading: boolean; onOpen: (documentId: string) => void }) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden", borderColor: "#d7e7ff" }}>
      <Box sx={{ p: 2, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography fontWeight={800}>Evidence</Typography>
        <Chip size="small" label={loading ? "Loading" : `${evidence.length} files`} />
      </Box>
      <Table aria-label="KYC evidence">
        <TableHead>
          <TableRow>
            <TableCell>Document</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Size</TableCell>
            <TableCell>Checksum</TableCell>
            <TableCell align="right">Review</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {evidence.map((document) => (
            <TableRow key={document.documentId}>
              <TableCell>{document.documentType === "IDENTITY_DOCUMENT" ? "KTP or SIM" : "Selfie"}</TableCell>
              <TableCell>{document.status}</TableCell>
              <TableCell>{Math.ceil(document.sizeBytes / 1024)} KB</TableCell>
              <TableCell>
                <Typography variant="body2" sx={{ fontFamily: "monospace" }}>{document.checksum.slice(0, 12)}...</Typography>
              </TableCell>
              <TableCell align="right">
                <Button variant="outlined" onClick={() => onOpen(document.documentId)}>Open evidence</Button>
              </TableCell>
            </TableRow>
          ))}
          {!loading && evidence.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5}>
                <Typography color="text.secondary">Select a pending KYC application to load evidence.</Typography>
              </TableCell>
            </TableRow>
          ) : null}
        </TableBody>
      </Table>
    </Paper>
  );
}

function KycApplicationsTable(props: {
  applications: KycApplication[];
  queueLoading: boolean;
  selectedId: string;
  onSelect: (applicationId: string) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden", borderColor: "#d7e7ff" }}>
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
