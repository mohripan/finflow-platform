package com.finflow.user.application;

import com.finflow.user.domain.UserProfile;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserProfileUseCase {
  UserProfile loadOrCreate(Jwt jwt);

  UserProfile update(Jwt jwt, UpdateProfileCommand command);

  UserProfile syncCustomerStatus(SyncCustomerStatusCommand command);
}
