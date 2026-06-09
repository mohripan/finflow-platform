package com.finflow.kyc.api;

import com.finflow.common.ApiResponse;
import com.finflow.common.CorrelationIds;
import com.finflow.kyc.application.KycDecisionAction;
import com.finflow.kyc.application.KycOnboardingUseCase;
import com.finflow.kyc.application.ReviewKycCommand;
import com.finflow.kyc.application.ConfirmDocumentUploadCommand;
import com.finflow.kyc.application.CreateDocumentUploadSessionCommand;
import com.finflow.kyc.application.SubmitKycCommand;
import com.finflow.kyc.domain.KycStatus;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kyc")
class KycController {
  private final KycOnboardingUseCase useCase;

  KycController(KycOnboardingUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<Object> me(JwtAuthenticationToken principal, @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    Object currentState = useCase.findMine(principal.getToken())
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
    var command = new SubmitKycCommand(
        request.legalName(),
        request.dateOfBirth(),
        request.nationalIdentityNumber(),
        request.phoneNumber(),
        request.address());
    return ApiResponse.ok(KycApplicationDto.from(useCase.submit(principal.getToken(), command)), correlationId);
  }

  @PostMapping("/me/applications")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<KycApplicationDto> createDraft(
      JwtAuthenticationToken principal,
      @Valid @RequestBody SubmitKycRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var command = new SubmitKycCommand(
        request.legalName(),
        request.dateOfBirth(),
        request.nationalIdentityNumber(),
        request.phoneNumber(),
        request.address());
    return ApiResponse.ok(KycApplicationDto.from(useCase.submit(principal.getToken(), command)), correlationId);
  }

  @PostMapping("/me/applications/{applicationId}/documents/upload-sessions")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<KycDocumentUploadSessionDto> createUploadSession(
      JwtAuthenticationToken principal,
      @PathVariable("applicationId") String applicationId,
      @Valid @RequestBody CreateKycDocumentUploadSessionRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var command = new CreateDocumentUploadSessionCommand(request.documentType(), request.contentType(), request.sizeBytes(), request.checksum());
    return ApiResponse.ok(KycDocumentUploadSessionDto.from(useCase.createDocumentUploadSession(principal.getToken(), applicationId, command)), correlationId);
  }

  @PostMapping("/me/applications/{applicationId}/documents/{documentId}/confirm-upload")
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<KycDocumentDto> confirmUpload(
      JwtAuthenticationToken principal,
      @PathVariable("applicationId") String applicationId,
      @PathVariable("documentId") String documentId,
      @Valid @RequestBody ConfirmKycDocumentUploadRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    return ApiResponse.ok(KycDocumentDto.from(useCase.confirmDocumentUpload(
        principal.getToken(),
        applicationId,
        documentId,
        new ConfirmDocumentUploadCommand(request.checksum()))), correlationId);
  }

  @PostMapping("/me/applications/{applicationId}/submissions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT')")
  ApiResponse<KycApplicationDto> submitForReview(
      JwtAuthenticationToken principal,
      @PathVariable("applicationId") String applicationId,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    return ApiResponse.ok(KycApplicationDto.from(useCase.submitForReview(principal.getToken(), applicationId)), correlationId);
  }

  @GetMapping("/admin/applications")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_COMPLIANCE','ROLE_SUPPORT')")
  ApiResponse<KycApplicationListDto> listApplications(
      @RequestParam(name = "status") Optional<KycStatus> status,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var applications = useCase.listForReview(status).stream()
        .map(KycApplicationDto::from)
        .toList();
    return ApiResponse.ok(new KycApplicationListDto(applications), correlationId);
  }

  @PostMapping("/admin/applications/{applicationId}/decisions")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_COMPLIANCE')")
  ApiResponse<KycApplicationDto> decide(
      JwtAuthenticationToken principal,
      @PathVariable("applicationId") String applicationId,
      @Valid @RequestBody AdminKycDecisionRequest request,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var command = new ReviewKycCommand(KycDecisionAction.valueOf(request.decision().name()), request.reason());
    return ApiResponse.ok(KycApplicationDto.from(useCase.decide(applicationId, command, principal.getToken(), correlationId)), correlationId);
  }

  @GetMapping("/admin/applications/{applicationId}/evidence")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_COMPLIANCE','ROLE_SUPPORT')")
  ApiResponse<KycDocumentListDto> listEvidence(
      @PathVariable("applicationId") String applicationId,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    var documents = useCase.listEvidence(applicationId).stream().map(KycDocumentDto::from).toList();
    return ApiResponse.ok(new KycDocumentListDto(documents), correlationId);
  }

  @PostMapping("/admin/applications/{applicationId}/evidence/{documentId}/review-url")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_COMPLIANCE','ROLE_SUPPORT')")
  ApiResponse<KycDocumentReviewUrlDto> createReviewUrl(
      @PathVariable("applicationId") String applicationId,
      @PathVariable("documentId") String documentId,
      @RequestHeader HttpHeaders headers) {
    var correlationId = CorrelationIds.ensure(headers.getFirst(CorrelationIds.HEADER));
    return ApiResponse.ok(KycDocumentReviewUrlDto.from(useCase.createReviewUrl(applicationId, documentId)), correlationId);
  }
}
