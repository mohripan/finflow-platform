package com.finflow.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfile {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String publicId;

  @Column(nullable = false, unique = true)
  private String keycloakSubject;

  @Column(nullable = false)
  private String email;

  private String phoneNumber;
  private String displayName;
  private String preferredLanguage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProfileStatus status;

  @Column(nullable = false, unique = true)
  private String customerPublicId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CustomerAccountStatus customerStatus;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserProfile() {
  }

  static UserProfile create(String subject, String email, String displayName, String phoneNumber) {
    var now = Instant.now();
    var profile = new UserProfile();
    profile.id = UUID.randomUUID();
    profile.publicId = PublicIds.next("usr");
    profile.keycloakSubject = subject;
    profile.email = email == null || email.isBlank() ? "unknown@" + subject + ".local" : email;
    profile.displayName = displayName;
    profile.phoneNumber = phoneNumber;
    profile.preferredLanguage = "en-US";
    profile.status = ProfileStatus.ACTIVE;
    profile.customerPublicId = PublicIds.next("cus");
    profile.customerStatus = CustomerAccountStatus.REGISTERED;
    profile.createdAt = now;
    profile.updatedAt = now;
    return profile;
  }

  void update(String displayName, String preferredLanguage) {
    if (displayName != null && !displayName.isBlank()) {
      this.displayName = displayName;
    }
    if (preferredLanguage != null && !preferredLanguage.isBlank()) {
      this.preferredLanguage = preferredLanguage;
    }
    this.updatedAt = Instant.now();
  }

  UUID id() { return id; }
  String publicId() { return publicId; }
  String keycloakSubject() { return keycloakSubject; }
  String email() { return email; }
  String phoneNumber() { return phoneNumber; }
  String displayName() { return displayName; }
  String preferredLanguage() { return preferredLanguage; }
  ProfileStatus status() { return status; }
  String customerPublicId() { return customerPublicId; }
  CustomerAccountStatus customerStatus() { return customerStatus; }
  Instant createdAt() { return createdAt; }
  Instant updatedAt() { return updatedAt; }
}
