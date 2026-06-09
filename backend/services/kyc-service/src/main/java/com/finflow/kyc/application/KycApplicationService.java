package com.finflow.kyc.application;

import com.finflow.kyc.domain.BusinessRuleException;
import com.finflow.kyc.domain.CustomerAccountStatus;
import com.finflow.kyc.domain.KycApplication;
import com.finflow.kyc.domain.KycDecision;
import com.finflow.kyc.domain.KycStatus;
import com.finflow.kyc.infrastructure.persistence.KycApplicationRepository;
import com.finflow.kyc.infrastructure.persistence.KycDecisionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KycApplicationService implements KycOnboardingUseCase {
  private final KycApplicationRepository repository;
  private final KycDecisionRepository decisionRepository;
  private final UserLifecyclePort userLifecyclePort;

  KycApplicationService(
      KycApplicationRepository repository,
      KycDecisionRepository decisionRepository,
      UserLifecyclePort userLifecyclePort) {
    this.repository = repository;
    this.decisionRepository = decisionRepository;
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
        .orElseGet(() -> repository.save(KycApplication.submit(
            jwt.getSubject(),
            command.legalName(),
            command.dateOfBirth(),
            command.nationalIdentityNumber(),
            command.phoneNumber(),
            command.address())));
  }

  @Override
  @Transactional(readOnly = true)
  public List<KycApplication> listForReview(Optional<KycStatus> status) {
    return status
        .map(repository::findByStatusOrderByUpdatedAtAsc)
        .orElseGet(repository::findAllByOrderByUpdatedAtDesc);
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
      case PENDING_REVIEW -> throw new IllegalArgumentException("Pending review is not an admin decision outcome.");
    };
  }
}
