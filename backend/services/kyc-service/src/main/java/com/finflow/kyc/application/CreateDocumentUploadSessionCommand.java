package com.finflow.kyc.application;

import com.finflow.kyc.domain.KycDocumentType;

public record CreateDocumentUploadSessionCommand(
    KycDocumentType documentType,
    String contentType,
    long sizeBytes,
    String checksum
) {
}
