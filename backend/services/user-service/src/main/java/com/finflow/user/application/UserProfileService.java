package com.finflow.user.application;

import com.finflow.user.domain.BusinessRuleException;
import com.finflow.user.domain.UserProfile;
import com.finflow.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserProfileService implements UserProfileUseCase {
  private final UserProfileRepository repository;

  UserProfileService(UserProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public UserProfile loadOrCreate(Jwt jwt) {
    return repository.findByKeycloakSubject(jwt.getSubject())
        .orElseGet(() -> repository.save(UserProfile.create(
            jwt.getSubject(),
            jwt.getClaimAsString("email"),
            firstPresent(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
            jwt.getClaimAsString("phone_number"))));
  }

  @Override
  @Transactional
  public UserProfile update(Jwt jwt, UpdateProfileCommand command) {
    var profile = loadOrCreate(jwt);
    profile.update(command.displayName(), command.preferredLanguage());
    return profile;
  }

  @Override
  @Transactional
  public UserProfile syncCustomerStatus(SyncCustomerStatusCommand command) {
    var profile = repository.findByKeycloakSubject(command.keycloakSubject())
        .orElseThrow(() -> new BusinessRuleException("USER_PROFILE_NOT_FOUND", "User profile must exist before KYC can update customer status."));
    profile.applyCustomerStatus(command.status());
    return profile;
  }

  private static String firstPresent(String first, String second) {
    return first != null && !first.isBlank() ? first : second;
  }
}
