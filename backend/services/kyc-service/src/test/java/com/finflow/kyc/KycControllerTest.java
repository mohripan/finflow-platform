package com.finflow.kyc;

import com.finflow.kyc.application.UserLifecyclePort;
import com.finflow.kyc.application.DocumentStoragePort;
import com.finflow.kyc.domain.CustomerAccountStatus;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class KycControllerTest {
  @Autowired
  MockMvc mvc;

  @MockBean
  UserLifecyclePort userLifecycleClient;

  @MockBean
  DocumentStoragePort documentStoragePort;

  @Test
  void returnsNotSubmittedBeforeApplicationExists() throws Exception {
    mvc.perform(get("/api/v1/kyc/me")
            .with(jwt().jwt(token -> token.subject("kyc-subject-1"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status", equalTo("NOT_SUBMITTED")));
  }

  @Test
  void validatesRequiredFields() throws Exception {
    mvc.perform(post("/api/v1/kyc/me/submissions")
            .with(jwt().jwt(token -> token.subject("kyc-subject-2"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"legalName\":\"\",\"dateOfBirth\":\"2030-01-01\",\"nationalIdentityNumber\":\"1\",\"phoneNumber\":\"abc\",\"address\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", equalTo("VALIDATION_ERROR")));
  }

  @Test
  void createsDraftKycWithoutReturningRawIdentityNumber() throws Exception {
    mockStorage();
    var body = """
        {
          "legalName": "Ayu Lestari",
          "dateOfBirth": "1996-05-20",
          "nationalIdentityNumber": "3173123456789000",
          "phoneNumber": "+6281234567890",
          "address": "Jakarta Selatan"
        }
        """;

    mvc.perform(post("/api/v1/kyc/me/applications")
            .with(jwt().jwt(token -> token.subject("kyc-subject-3"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.status", equalTo("DRAFT")))
        .andExpect(content().string(not(containsString("3173123456789000"))));
  }

  @Test
  void adminApprovesPendingKycAndSyncsCustomerStatus() throws Exception {
    mockStorage();
    var body = """
        {
          "legalName": "Dewi Santoso",
          "dateOfBirth": "1994-04-18",
          "nationalIdentityNumber": "3173123456789001",
          "phoneNumber": "+6281234567891",
          "address": "Bandung"
        }
        """;

    var draftResult = mvc.perform(post("/api/v1/kyc/me/applications")
            .with(jwt().jwt(token -> token.subject("kyc-subject-4"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();

    var applicationId = com.jayway.jsonpath.JsonPath.read(
        draftResult.getResponse().getContentAsString(), "$.data.applicationId").toString();

    mvc.perform(post("/api/v1/kyc/me/applications/{applicationId}/submissions", applicationId)
            .with(jwt().jwt(token -> token.subject("kyc-subject-4"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error.code", equalTo("KYC_EVIDENCE_REQUIRED")));

    uploadAndConfirm(applicationId, "kyc-subject-4", "IDENTITY_DOCUMENT", "identity-checksum");
    uploadAndConfirm(applicationId, "kyc-subject-4", "SELFIE", "selfie-checksum");

    mvc.perform(post("/api/v1/kyc/me/applications/{applicationId}/submissions", applicationId)
            .with(jwt().jwt(token -> token.subject("kyc-subject-4"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status", equalTo("PENDING_REVIEW")));

    mvc.perform(get("/api/v1/kyc/admin/applications")
            .param("status", "PENDING_REVIEW")
            .with(jwt().jwt(token -> token.subject("support-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.applications[0].applicationId", equalTo(applicationId)));

    var evidenceResult = mvc.perform(get("/api/v1/kyc/admin/applications/{applicationId}/evidence", applicationId)
            .with(jwt().jwt(token -> token.subject("support-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.documents[0].status", equalTo("UPLOADED")))
        .andReturn();

    var documentId = com.jayway.jsonpath.JsonPath.read(
        evidenceResult.getResponse().getContentAsString(), "$.data.documents[0].documentId").toString();

    mvc.perform(post("/api/v1/kyc/admin/applications/{applicationId}/evidence/{documentId}/review-url", applicationId, documentId)
            .with(jwt().jwt(token -> token.subject("support-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviewUrl", equalTo("http://storage.test/review")));

    mvc.perform(post("/api/v1/kyc/admin/applications/{applicationId}/decisions", applicationId)
            .with(jwt().jwt(token -> token.subject("admin-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVE\",\"reason\":\"Identity document matches selfie.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status", equalTo("APPROVED")))
        .andExpect(jsonPath("$.data.reviewedBy", equalTo("admin-subject")));

    verify(userLifecycleClient).syncCustomerStatus(
        eq("kyc-subject-4"),
        eq(CustomerAccountStatus.KYC_APPROVED),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.anyString());

    mvc.perform(post("/api/v1/kyc/admin/applications/{applicationId}/decisions", applicationId)
            .with(jwt().jwt(token -> token.subject("admin-subject"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"REJECT\",\"reason\":\"Late duplicate review.\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error.code", equalTo("KYC_NOT_PENDING_REVIEW")));
  }

  private void uploadAndConfirm(String applicationId, String subject, String documentType, String checksum) throws Exception {
    var uploadResult = mvc.perform(post("/api/v1/kyc/me/applications/{applicationId}/documents/upload-sessions", applicationId)
            .with(jwt().jwt(token -> token.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "documentType": "%s",
                  "contentType": "image/jpeg",
                  "sizeBytes": 1200,
                  "checksum": "%s"
                }
                """.formatted(documentType, checksum)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.uploadUrl", equalTo("http://storage.test/upload")))
        .andReturn();
    var documentId = com.jayway.jsonpath.JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.documentId").toString();

    mvc.perform(post("/api/v1/kyc/me/applications/{applicationId}/documents/{documentId}/confirm-upload", applicationId, documentId)
            .with(jwt().jwt(token -> token.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"checksum\":\"" + checksum + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status", equalTo("UPLOADED")));
  }

  private void mockStorage() {
    when(documentStoragePort.createUpload(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new DocumentStoragePort.PresignedUpload("finflow-kyc", "kyc/test/object", "http://storage.test/upload", java.time.Instant.parse("2026-06-09T00:00:00Z")));
    when(documentStoragePort.verifyUploaded(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(new DocumentStoragePort.StoredObject(1200, "image/jpeg"));
    when(documentStoragePort.createReviewUrl(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new DocumentStoragePort.PresignedDownload("http://storage.test/review", java.time.Instant.parse("2026-06-09T00:00:00Z")));
  }
}
