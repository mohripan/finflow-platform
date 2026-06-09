package com.finflow.user.api;

import com.finflow.common.ApiResponse;
import com.finflow.common.CorrelationIds;
import com.finflow.user.application.SyncCustomerStatusCommand;
import com.finflow.user.application.UpdateProfileCommand;
import com.finflow.user.application.UserProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class UserController {
  private final UserProfileUseCase useCase;

  UserController(UserProfileUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT','ROLE_ADMIN','ROLE_SUPPORT','ROLE_COMPLIANCE')")
  ApiResponse<UserProfileDto> me(
      JwtAuthenticationToken principal,
      @RequestHeader HttpHeaders headers) {
    Jwt jwt = principal.getToken();
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    return ApiResponse.ok(UserProfileDto.from(useCase.loadOrCreate(jwt)), correlationId);
  }

  @PatchMapping("/me")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<UserProfileDto> update(
      JwtAuthenticationToken principal,
      @Valid @RequestBody UpdateProfileRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var command = new UpdateProfileCommand(request.displayName(), request.preferredLanguage());
    return ApiResponse.ok(UserProfileDto.from(useCase.update(principal.getToken(), command)), correlationId);
  }

  @PostMapping("/internal/customer-status")
  @PreAuthorize("hasAnyAuthority('ROLE_SERVICE','ROLE_ADMIN','ROLE_COMPLIANCE')")
  ApiResponse<UserProfileDto> syncCustomerStatus(
      @Valid @RequestBody SyncCustomerStatusRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var command = new SyncCustomerStatusCommand(request.keycloakSubject(), request.status());
    return ApiResponse.ok(UserProfileDto.from(useCase.syncCustomerStatus(command)), correlationId);
  }
}
