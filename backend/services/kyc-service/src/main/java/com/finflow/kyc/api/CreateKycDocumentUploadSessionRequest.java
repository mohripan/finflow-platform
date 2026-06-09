package com.finflow.kyc.api;

import com.finflow.kyc.domain.KycDocumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateKycDocumentUploadSessionRequest(
    @NotNull KycDocumentType documentType,
    @NotBlank String contentType,
    @Min(1) @Max(8_000_000) long sizeBytes,
    @NotBlank String checksum
) {
}
