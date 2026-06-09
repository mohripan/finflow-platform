export type AuthState = "loading" | "anonymous" | "authenticated" | "error";

export type KycApplication = {
  applicationId: string;
  status: string;
  legalName: string;
  dateOfBirth: string;
  phoneNumber: string;
  address: string;
  rejectionCount: number;
  reviewedBy?: string | null;
  reviewReason?: string | null;
};

export type KycDecision = "APPROVE" | "REJECT" | "REQUEST_RESUBMISSION";
