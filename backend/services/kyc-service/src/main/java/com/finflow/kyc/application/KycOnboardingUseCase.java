package com.finflow.kyc.application;

import com.finflow.kyc.domain.KycApplication;
import com.finflow.kyc.domain.KycStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;

public interface KycOnboardingUseCase {
  Optional<KycApplication> findMine(Jwt jwt);

  KycApplication submit(Jwt jwt, SubmitKycCommand command);

  List<KycApplication> listForReview(Optional<KycStatus> status);

  KycApplication decide(String applicationId, ReviewKycCommand command, Jwt adminJwt, String correlationId);
}
