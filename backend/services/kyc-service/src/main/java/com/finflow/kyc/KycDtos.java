package com.finflow.kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

record SubmitKycRequest(
    @NotBlank @Size(max = 120) String legalName,
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(min = 8, max = 32) String nationalIdentityNumber,
    @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,16}$") String phoneNumber,
    @NotBlank @Size(max = 500) String address
) {
}

record KycApplicationDto(
    String applicationId,
    String status,
    String legalName,
    LocalDate dateOfBirth,
    String phoneNumber,
    String address,
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
        application.createdAt(),
        application.updatedAt());
  }
}

record KycStatusDto(String status) {
}
