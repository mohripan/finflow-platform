package com.finflow.kyc.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmKycDocumentUploadRequest(@NotBlank String checksum) {
}
