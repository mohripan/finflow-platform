package com.finflow.kyc.application;

import com.finflow.kyc.domain.KycDocument;
import java.time.Instant;

public record DocumentUploadSession(
    KycDocument document,
    String uploadUrl,
    Instant expiresAt
) {
}
