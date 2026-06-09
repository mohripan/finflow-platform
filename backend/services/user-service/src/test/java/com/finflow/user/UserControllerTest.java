package com.finflow.user;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class UserControllerTest {
  @Autowired
  MockMvc mvc;

  @Autowired
  UserProfileRepository repository;

  @Test
  void rejectsUnauthenticatedProfileAccess() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createsAndLoadsProfileIdempotentlyForJwtSubject() throws Exception {
    var jwt = jwt()
        .jwt(token -> token.subject("kc-subject-1").claim("email", "ayu@example.test").claim("name", "Ayu"))
        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

    mvc.perform(get("/api/v1/users/me").with(jwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email", equalTo("ayu@example.test")))
        .andExpect(jsonPath("$.data.customerAccount.status", equalTo("REGISTERED")));

    mvc.perform(get("/api/v1/users/me").with(jwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email", equalTo("ayu@example.test")));

    org.assertj.core.api.Assertions.assertThat(repository.findAll()).hasSize(1);
  }

  @Test
  void updatesProfilePreferences() throws Exception {
    mvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(token -> token.subject("kc-subject-2").claim("email", "bima@example.test"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Bima\",\"preferredLanguage\":\"id-ID\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName", equalTo("Bima")))
        .andExpect(jsonPath("$.data.preferredLanguage", equalTo("id-ID")));
  }
}
