package com.finflow.kyc.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SubmitKycRequest(
    @NotBlank @Size(max = 120) String legalName,
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(min = 8, max = 32) String nationalIdentityNumber,
    @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,16}$") String phoneNumber,
    @NotBlank @Size(max = 500) String address
) {
}
