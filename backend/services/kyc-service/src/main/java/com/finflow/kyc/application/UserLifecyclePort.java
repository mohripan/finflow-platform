package com.finflow.kyc.application;

import com.finflow.kyc.domain.CustomerAccountStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserLifecyclePort {
  void syncCustomerStatus(String keycloakSubject, CustomerAccountStatus status, Jwt adminJwt, String correlationId);
}
