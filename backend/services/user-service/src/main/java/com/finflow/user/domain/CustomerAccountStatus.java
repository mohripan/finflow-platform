package com.finflow.user.domain;

public enum CustomerAccountStatus {
  REGISTERED,
  KYC_IN_REVIEW,
  KYC_APPROVED,
  WALLET_ACTIVE,
  FROZEN,
  CLOSED,
  KYC_REJECTED,
  KYC_RESUBMISSION_REQUIRED,
  KYC_LOCKED
}
