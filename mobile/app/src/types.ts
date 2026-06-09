export type ApiProfile = {
  userId: string;
  email: string;
  displayName?: string;
  status: string;
  customerAccount: { customerId: string; status: string };
};

export type KycState = {
  status: string;
  applicationId?: string;
  legalName?: string;
  phoneNumber?: string;
};

export type KycForm = {
  legalName: string;
  dateOfBirth: string;
  nationalIdentityNumber: string;
  phoneNumber: string;
  address: string;
};

export const emptyKycForm: KycForm = {
  legalName: "",
  dateOfBirth: "",
  nationalIdentityNumber: "",
  phoneNumber: "",
  address: ""
};

export type KycDocumentType = "IDENTITY_DOCUMENT" | "SELFIE";

export type CapturedEvidence = {
  documentType: KycDocumentType;
  uri: string;
  contentType: "image/jpeg" | "image/png";
  sizeBytes: number;
  checksum: string;
};

export type KycDocumentUploadSession = {
  documentId: string;
  documentType: KycDocumentType;
  uploadUrl: string;
  expiresAt: string;
};
