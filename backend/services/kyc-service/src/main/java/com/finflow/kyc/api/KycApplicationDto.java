package com.finflow.kyc.api;

import com.finflow.kyc.domain.KycApplication;
import java.time.Instant;
import java.time.LocalDate;

public record KycApplicationDto(
    String applicationId,
    String status,
    String legalName,
    LocalDate dateOfBirth,
    String phoneNumber,
    String address,
    int rejectionCount,
    String reviewedBy,
    String reviewReason,
    Instant reviewedAt,
    Instant createdAt,
    Instant updatedAt
) {
  static KycApplicationDto from(KycApplication application) {
    return new KycApplicationDto(
        application.publicId(),
        application.status().name(),
        application.legalName(),
        application.dateOfBirth(),
        application.phoneNumber(),
        application.address(),
        application.rejectionCount(),
        application.reviewedBy(),
        application.reviewReason(),
        application.reviewedAt(),
        application.createdAt(),
        application.updatedAt());
  }
}
