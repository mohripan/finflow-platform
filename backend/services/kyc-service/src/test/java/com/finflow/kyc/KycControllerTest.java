package com.finflow.kyc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
  void submitsKycWithoutReturningRawIdentityNumber() throws Exception {
    var body = """
        {
          "legalName": "Ayu Lestari",
          "dateOfBirth": "1996-05-20",
          "nationalIdentityNumber": "3173123456789000",
          "phoneNumber": "+6281234567890",
          "address": "Jakarta Selatan"
        }
        """;

    mvc.perform(post("/api/v1/kyc/me/submissions")
            .with(jwt().jwt(token -> token.subject("kyc-subject-3"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status", equalTo("PENDING_REVIEW")))
        .andExpect(content().string(not(containsString("3173123456789000"))));
  }
}
