package com.finflow.kyc.application;

import java.time.Instant;

public record DocumentReviewUrl(
    String documentId,
    String reviewUrl,
    Instant expiresAt
) {
}
