package com.finflow.kyc.application;

import com.finflow.kyc.domain.KycApplication;
import com.finflow.kyc.domain.KycDocument;
import com.finflow.kyc.domain.KycStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;

public interface KycOnboardingUseCase {
  Optional<KycApplication> findMine(Jwt jwt);

  KycApplication submit(Jwt jwt, SubmitKycCommand command);

  DocumentUploadSession createDocumentUploadSession(Jwt jwt, String applicationId, CreateDocumentUploadSessionCommand command);

  KycDocument confirmDocumentUpload(Jwt jwt, String applicationId, String documentId, ConfirmDocumentUploadCommand command);

  KycApplication submitForReview(Jwt jwt, String applicationId);

  List<KycApplication> listForReview(Optional<KycStatus> status);

  List<KycDocument> listEvidence(String applicationId);

  DocumentReviewUrl createReviewUrl(String applicationId, String documentId);

  KycApplication decide(String applicationId, ReviewKycCommand command, Jwt adminJwt, String correlationId);
}
