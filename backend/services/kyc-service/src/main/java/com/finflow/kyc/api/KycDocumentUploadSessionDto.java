package com.finflow.kyc.api;

import com.finflow.kyc.application.DocumentUploadSession;
import java.time.Instant;

public record KycDocumentUploadSessionDto(
    String documentId,
    String documentType,
    String uploadUrl,
    Instant expiresAt
) {
  static KycDocumentUploadSessionDto from(DocumentUploadSession session) {
    return new KycDocumentUploadSessionDto(
        session.document().publicId(),
        session.document().documentType().name(),
        session.uploadUrl(),
        session.expiresAt());
  }
}
