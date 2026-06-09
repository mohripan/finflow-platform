package com.finflow.kyc.domain;

public enum KycStatus {
  DRAFT,
  PENDING_REVIEW,
  APPROVED,
  REJECTED,
  RESUBMISSION_REQUIRED,
  LOCKED
}
