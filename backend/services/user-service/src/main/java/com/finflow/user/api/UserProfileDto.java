package com.finflow.user.api;

import com.finflow.user.domain.UserProfile;
import java.time.Instant;

public record UserProfileDto(
    String userId,
    String email,
    String phoneNumber,
    String displayName,
    String preferredLanguage,
    String status,
    CustomerAccountDto customerAccount,
    Instant createdAt,
    Instant updatedAt
) {
  static UserProfileDto from(UserProfile profile) {
    return new UserProfileDto(
        profile.publicId(),
        profile.email(),
        profile.phoneNumber(),
        profile.displayName(),
        profile.preferredLanguage(),
        profile.status().name(),
        new CustomerAccountDto(profile.customerPublicId(), profile.customerStatus().name()),
        profile.createdAt(),
        profile.updatedAt());
  }
}
