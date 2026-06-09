package com.finflow.kyc.application;

import java.time.LocalDate;

public record SubmitKycCommand(
    String legalName,
    LocalDate dateOfBirth,
    String nationalIdentityNumber,
    String phoneNumber,
    String address
) {
}
