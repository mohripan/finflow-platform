package com.finflow.kyc.api;

import com.finflow.kyc.application.DocumentReviewUrl;
import java.time.Instant;

public record KycDocumentReviewUrlDto(
    String documentId,
    String reviewUrl,
    Instant expiresAt
) {
  static KycDocumentReviewUrlDto from(DocumentReviewUrl reviewUrl) {
    return new KycDocumentReviewUrlDto(reviewUrl.documentId(), reviewUrl.reviewUrl(), reviewUrl.expiresAt());
  }
}
