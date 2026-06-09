package com.finflow.kyc.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminKycDecisionRequest(
    @NotNull AdminKycDecision decision,
    @NotBlank @Size(max = 500) String reason
) {
}
