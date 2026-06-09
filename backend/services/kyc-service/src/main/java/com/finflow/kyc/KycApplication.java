package com.finflow.kyc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "kyc_applications")
class KycApplication {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String publicId;

  @Column(nullable = false)
  private String keycloakSubject;

  @Column(nullable = false)
  private String legalName;

  @Column(nullable = false)
  private LocalDate dateOfBirth;

  @Column(nullable = false)
  private String nationalIdentityHash;

  @Column(nullable = false)
  private String phoneNumber;

  @Column(nullable = false)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KycStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected KycApplication() {
  }

  static KycApplication submit(String subject, SubmitKycRequest request) {
    var now = Instant.now();
    var application = new KycApplication();
    application.id = UUID.randomUUID();
    application.publicId = PublicIds.next("kyc");
    application.keycloakSubject = subject;
    application.legalName = request.legalName().trim();
    application.dateOfBirth = request.dateOfBirth();
    application.nationalIdentityHash = sha256(request.nationalIdentityNumber().trim());
    application.phoneNumber = request.phoneNumber().trim();
    application.address = request.address().trim();
    application.status = KycStatus.PENDING_REVIEW;
    application.createdAt = now;
    application.updatedAt = now;
    return application;
  }

  void resubmit(SubmitKycRequest request) {
    if (status == KycStatus.LOCKED) {
      throw new BusinessRuleException("REJECTION_LIMIT_REACHED", "KYC application is locked after repeated rejection.");
    }
    this.legalName = request.legalName().trim();
    this.dateOfBirth = request.dateOfBirth();
    this.nationalIdentityHash = sha256(request.nationalIdentityNumber().trim());
    this.phoneNumber = request.phoneNumber().trim();
    this.address = request.address().trim();
    this.status = KycStatus.PENDING_REVIEW;
    this.updatedAt = Instant.now();
  }

  private static String sha256(String input) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  String publicId() { return publicId; }
  String keycloakSubject() { return keycloakSubject; }
  String legalName() { return legalName; }
  LocalDate dateOfBirth() { return dateOfBirth; }
  String phoneNumber() { return phoneNumber; }
  String address() { return address; }
  KycStatus status() { return status; }
  Instant createdAt() { return createdAt; }
  Instant updatedAt() { return updatedAt; }
}
