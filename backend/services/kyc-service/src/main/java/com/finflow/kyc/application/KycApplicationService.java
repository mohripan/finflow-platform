package com.finflow.kyc.application;

import com.finflow.kyc.domain.BusinessRuleException;
import com.finflow.kyc.domain.CustomerAccountStatus;
import com.finflow.kyc.domain.KycApplication;
import com.finflow.kyc.domain.KycDecision;
import com.finflow.kyc.domain.KycDocument;
import com.finflow.kyc.domain.KycDocumentStatus;
import com.finflow.kyc.domain.KycDocumentType;
import com.finflow.kyc.domain.KycStatus;
import com.finflow.kyc.infrastructure.persistence.KycApplicationRepository;
import com.finflow.kyc.infrastructure.persistence.KycDecisionRepository;
import com.finflow.kyc.infrastructure.persistence.KycDocumentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KycApplicationService implements KycOnboardingUseCase {
  private final KycApplicationRepository repository;
  private final KycDecisionRepository decisionRepository;
  private final KycDocumentRepository documentRepository;
  private final DocumentStoragePort documentStoragePort;
  private final UserLifecyclePort userLifecyclePort;

  KycApplicationService(
      KycApplicationRepository repository,
      KycDecisionRepository decisionRepository,
      KycDocumentRepository documentRepository,
      DocumentStoragePort documentStoragePort,
      UserLifecyclePort userLifecyclePort) {
    this.repository = repository;
    this.decisionRepository = decisionRepository;
    this.documentRepository = documentRepository;
    this.documentStoragePort = documentStoragePort;
    this.userLifecyclePort = userLifecyclePort;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<KycApplication> findMine(Jwt jwt) {
    return repository.findByKeycloakSubject(jwt.getSubject());
  }

  @Override
  @Transactional
  public KycApplication submit(Jwt jwt, SubmitKycCommand command) {
    return repository.findByKeycloakSubject(jwt.getSubject())
        .map(existing -> {
          existing.resubmit(
              command.legalName(),
              command.dateOfBirth(),
              command.nationalIdentityNumber(),
              command.phoneNumber(),
              command.address());
          return existing;
        })
        .orElseGet(() -> repository.save(KycApplication.draft(
            jwt.getSubject(),
            command.legalName(),
            command.dateOfBirth(),
            command.nationalIdentityNumber(),
            command.phoneNumber(),
            command.address())));
  }

  @Override
  @Transactional
  public DocumentUploadSession createDocumentUploadSession(Jwt jwt, String applicationId, CreateDocumentUploadSessionCommand command) {
    var application = findOwnedApplication(jwt, applicationId);
    validateDocumentUpload(command);
    var objectKey = "kyc/%s/%s/%s".formatted(application.publicId(), command.documentType().name().toLowerCase(), java.util.UUID.randomUUID());
    var upload = documentStoragePort.createUpload(objectKey, command.contentType());
    var document = documentRepository.save(KycDocument.requested(
        application.publicId(),
        jwt.getSubject(),
        command.documentType(),
        upload.bucket(),
        upload.objectKey(),
        command.contentType(),
        command.sizeBytes(),
        command.checksum()));
    return new DocumentUploadSession(document, upload.uploadUrl(), upload.expiresAt());
  }

  @Override
  @Transactional
  public KycDocument confirmDocumentUpload(Jwt jwt, String applicationId, String documentId, ConfirmDocumentUploadCommand command) {
    findOwnedApplication(jwt, applicationId);
    var document = documentRepository.findByPublicIdAndApplicationPublicId(documentId, applicationId)
        .orElseThrow(() -> new BusinessRuleException("KYC_DOCUMENT_NOT_FOUND", "KYC document was not found."));
    if (!document.keycloakSubject().equals(jwt.getSubject())) {
      throw new BusinessRuleException("KYC_DOCUMENT_FORBIDDEN", "KYC document does not belong to the current customer.");
    }
    documentStoragePort.verifyUploaded(document.objectKey(), document.sizeBytes());
    document.confirmUploaded(command.checksum());
    return document;
  }

  @Override
  @Transactional
  public KycApplication submitForReview(Jwt jwt, String applicationId) {
    var application = findOwnedApplication(jwt, applicationId);
    application.submitForReview(
        documentRepository.existsByApplicationPublicIdAndDocumentTypeAndStatus(applicationId, KycDocumentType.IDENTITY_DOCUMENT, KycDocumentStatus.UPLOADED),
        documentRepository.existsByApplicationPublicIdAndDocumentTypeAndStatus(applicationId, KycDocumentType.SELFIE, KycDocumentStatus.UPLOADED));
    return application;
  }

  @Override
  @Transactional(readOnly = true)
  public List<KycApplication> listForReview(Optional<KycStatus> status) {
    return status
        .map(repository::findByStatusOrderByUpdatedAtAsc)
        .orElseGet(repository::findAllByOrderByUpdatedAtDesc);
  }

  @Override
  @Transactional(readOnly = true)
  public List<KycDocument> listEvidence(String applicationId) {
    repository.findByPublicId(applicationId)
        .orElseThrow(() -> new BusinessRuleException("KYC_APPLICATION_NOT_FOUND", "KYC application was not found."));
    return documentRepository.findByApplicationPublicIdOrderByCreatedAtAsc(applicationId);
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentReviewUrl createReviewUrl(String applicationId, String documentId) {
    var document = documentRepository.findByPublicIdAndApplicationPublicId(documentId, applicationId)
        .orElseThrow(() -> new BusinessRuleException("KYC_DOCUMENT_NOT_FOUND", "KYC document was not found."));
    var review = documentStoragePort.createReviewUrl(document.objectKey());
    return new DocumentReviewUrl(document.publicId(), review.reviewUrl(), review.expiresAt());
  }

  @Override
  @Transactional
  public KycApplication decide(String applicationId, ReviewKycCommand command, Jwt adminJwt, String correlationId) {
    var application = repository.findByPublicId(applicationId)
        .orElseThrow(() -> new BusinessRuleException("KYC_APPLICATION_NOT_FOUND", "KYC application was not found."));
    var actor = adminJwt.getSubject();
    KycStatus beforeStatus = switch (command.decision()) {
      case APPROVE -> application.approve(actor, command.reason());
      case REJECT -> application.reject(actor, command.reason());
      case REQUEST_RESUBMISSION -> application.requestResubmission(actor, command.reason());
    };
    decisionRepository.save(KycDecision.record(application, beforeStatus, actor, command.reason()));
    userLifecyclePort.syncCustomerStatus(
        application.keycloakSubject(),
        toCustomerStatus(application.status()),
        adminJwt,
        correlationId);
    return application;
  }

  private CustomerAccountStatus toCustomerStatus(KycStatus status) {
    return switch (status) {
      case APPROVED -> CustomerAccountStatus.KYC_APPROVED;
      case REJECTED -> CustomerAccountStatus.KYC_REJECTED;
      case RESUBMISSION_REQUIRED -> CustomerAccountStatus.KYC_RESUBMISSION_REQUIRED;
      case LOCKED -> CustomerAccountStatus.KYC_LOCKED;
      case DRAFT, PENDING_REVIEW -> throw new IllegalArgumentException("Draft or pending review is not an admin decision outcome.");
    };
  }

  private KycApplication findOwnedApplication(Jwt jwt, String applicationId) {
    var application = repository.findByPublicId(applicationId)
        .orElseThrow(() -> new BusinessRuleException("KYC_APPLICATION_NOT_FOUND", "KYC application was not found."));
    if (!application.keycloakSubject().equals(jwt.getSubject())) {
      throw new BusinessRuleException("KYC_APPLICATION_FORBIDDEN", "KYC application does not belong to the current customer.");
    }
    return application;
  }

  private void validateDocumentUpload(CreateDocumentUploadSessionCommand command) {
    if (!command.contentType().equals("image/jpeg") && !command.contentType().equals("image/png")) {
      throw new BusinessRuleException("KYC_DOCUMENT_CONTENT_TYPE_UNSUPPORTED", "KYC document uploads must be JPEG or PNG images.");
    }
    if (command.sizeBytes() <= 0 || command.sizeBytes() > 8_000_000) {
      throw new BusinessRuleException("KYC_DOCUMENT_SIZE_INVALID", "KYC document uploads must be between 1 byte and 8 MB.");
    }
    if (command.checksum() == null || command.checksum().isBlank()) {
      throw new BusinessRuleException("KYC_DOCUMENT_CHECKSUM_REQUIRED", "KYC document checksum is required.");
    }
  }
}
