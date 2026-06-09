package com.finflow.kyc;

import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KycApplicationService {
  private final KycApplicationRepository repository;

  KycApplicationService(KycApplicationRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  Optional<KycApplication> findMine(Jwt jwt) {
    return repository.findByKeycloakSubject(jwt.getSubject());
  }

  @Transactional
  KycApplication submit(Jwt jwt, SubmitKycRequest request) {
    return repository.findByKeycloakSubject(jwt.getSubject())
        .map(existing -> {
          existing.resubmit(request);
          return existing;
        })
        .orElseGet(() -> repository.save(KycApplication.submit(jwt.getSubject(), request)));
  }
}
