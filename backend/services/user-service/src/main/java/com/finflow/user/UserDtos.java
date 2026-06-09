package com.finflow.user;

import jakarta.validation.constraints.Size;
import java.time.Instant;

record UpdateProfileRequest(
    @Size(max = 120) String displayName,
    @Size(max = 16) String preferredLanguage
) {
}

record UserProfileDto(
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

record CustomerAccountDto(String customerId, String status) {
}
