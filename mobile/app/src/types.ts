export type ApiProfile = {
  userId: string;
  email: string;
  displayName?: string;
  status: string;
  customerAccount: { customerId: string; status: string };
};

export type KycState = {
  status: string;
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
