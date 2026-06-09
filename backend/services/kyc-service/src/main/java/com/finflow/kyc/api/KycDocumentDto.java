package com.finflow.kyc.api;

import com.finflow.kyc.domain.KycDocument;
import java.time.Instant;

public record KycDocumentDto(
    String documentId,
    String documentType,
    String status,
    String contentType,
    long sizeBytes,
    String checksum,
    Instant createdAt,
    Instant updatedAt
) {
  static KycDocumentDto from(KycDocument document) {
    return new KycDocumentDto(
        document.publicId(),
        document.documentType().name(),
        document.status().name(),
        document.contentType(),
        document.sizeBytes(),
        document.checksum(),
        document.createdAt(),
        document.updatedAt());
  }
}
