package com.finflow.kyc.infrastructure.user;

import com.finflow.kyc.application.UserLifecyclePort;
import com.finflow.kyc.domain.CustomerAccountStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class RestUserLifecycleClient implements UserLifecyclePort {
  private final RestClient restClient;

  RestUserLifecycleClient(
      RestClient.Builder builder,
      @Value("${finflow.user-service.base-url:http://localhost:8081}") String userServiceBaseUrl) {
    this.restClient = builder.baseUrl(userServiceBaseUrl).build();
  }

  @Override
  public void syncCustomerStatus(String keycloakSubject, CustomerAccountStatus status, Jwt adminJwt, String correlationId) {
    restClient.post()
        .uri("/api/v1/users/internal/customer-status")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt.getTokenValue())
        .header("X-Correlation-Id", correlationId)
        .body(new SyncCustomerStatusRequest(keycloakSubject, status))
        .retrieve()
        .toBodilessEntity();
  }

  private record SyncCustomerStatusRequest(String keycloakSubject, CustomerAccountStatus status) {
  }
}
