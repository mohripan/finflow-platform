package com.finflow.user;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserProfileService {
  private final UserProfileRepository repository;

  UserProfileService(UserProfileRepository repository) {
    this.repository = repository;
  }

  @Transactional
  UserProfile loadOrCreate(Jwt jwt) {
    return repository.findByKeycloakSubject(jwt.getSubject())
        .orElseGet(() -> repository.save(UserProfile.create(
            jwt.getSubject(),
            jwt.getClaimAsString("email"),
            firstPresent(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
            jwt.getClaimAsString("phone_number"))));
  }

  @Transactional
  UserProfile update(Jwt jwt, UpdateProfileRequest request) {
    var profile = loadOrCreate(jwt);
    profile.update(request.displayName(), request.preferredLanguage());
    return profile;
  }

  private static String firstPresent(String first, String second) {
    return first != null && !first.isBlank() ? first : second;
  }
}
