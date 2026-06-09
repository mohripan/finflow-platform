package com.finflow.user.domain;

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
public class UserProfile {
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

  public static UserProfile create(String subject, String email, String displayName, String phoneNumber) {
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

  public void update(String displayName, String preferredLanguage) {
    if (displayName != null && !displayName.isBlank()) {
      this.displayName = displayName;
    }
    if (preferredLanguage != null && !preferredLanguage.isBlank()) {
      this.preferredLanguage = preferredLanguage;
    }
    this.updatedAt = Instant.now();
  }

  public void applyCustomerStatus(CustomerAccountStatus nextStatus) {
    if (this.customerStatus == CustomerAccountStatus.CLOSED) {
      throw new BusinessRuleException("CUSTOMER_ACCOUNT_CLOSED", "Closed customer accounts cannot change onboarding status.");
    }
    if (this.customerStatus == CustomerAccountStatus.WALLET_ACTIVE && nextStatus != CustomerAccountStatus.FROZEN) {
      throw new BusinessRuleException("CUSTOMER_WALLET_ALREADY_ACTIVE", "Wallet-active customer accounts cannot move back to KYC status.");
    }
    this.customerStatus = nextStatus;
    this.updatedAt = Instant.now();
  }

  public UUID id() { return id; }
  public String publicId() { return publicId; }
  public String keycloakSubject() { return keycloakSubject; }
  public String email() { return email; }
  public String phoneNumber() { return phoneNumber; }
  public String displayName() { return displayName; }
  public String preferredLanguage() { return preferredLanguage; }
  public ProfileStatus status() { return status; }
  public String customerPublicId() { return customerPublicId; }
  public CustomerAccountStatus customerStatus() { return customerStatus; }
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
}
