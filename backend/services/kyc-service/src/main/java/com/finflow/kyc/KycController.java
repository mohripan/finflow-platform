package com.finflow.kyc;

import com.finflow.common.ApiResponse;
import com.finflow.common.CorrelationIds;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kyc")
class KycController {
  private final KycApplicationService service;

  KycController(KycApplicationService service) {
    this.service = service;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<Object> me(JwtAuthenticationToken principal, @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    Object currentState = service.findMine(principal.getToken())
        .<Object>map(KycApplicationDto::from)
        .orElse(new KycStatusDto("NOT_SUBMITTED"));
    return ApiResponse.ok(currentState, correlationId);
  }

  @PostMapping("/me/submissions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<KycApplicationDto> submit(
      JwtAuthenticationToken principal,
      @Valid @RequestBody SubmitKycRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    return ApiResponse.ok(KycApplicationDto.from(service.submit(principal.getToken(), request)), correlationId);
  }
}
