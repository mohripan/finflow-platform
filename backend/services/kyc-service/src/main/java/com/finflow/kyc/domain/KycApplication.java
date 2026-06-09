package com.finflow.kyc.domain;

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
public class KycApplication {
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

  @Column(nullable = false)
  private int rejectionCount;

  private Instant reviewedAt;
  private String reviewedBy;

  @Column(length = 500)
  private String reviewReason;

  protected KycApplication() {
  }

  public static KycApplication draft(
      String subject,
      String legalName,
      LocalDate dateOfBirth,
      String nationalIdentityNumber,
      String phoneNumber,
      String address) {
    var now = Instant.now();
    var application = new KycApplication();
    application.id = UUID.randomUUID();
    application.publicId = PublicIds.next("kyc");
    application.keycloakSubject = subject;
    application.legalName = legalName.trim();
    application.dateOfBirth = dateOfBirth;
    application.nationalIdentityHash = sha256(nationalIdentityNumber.trim());
    application.phoneNumber = phoneNumber.trim();
    application.address = address.trim();
    application.status = KycStatus.DRAFT;
    application.rejectionCount = 0;
    application.createdAt = now;
    application.updatedAt = now;
    return application;
  }

  public void resubmit(
      String legalName,
      LocalDate dateOfBirth,
      String nationalIdentityNumber,
      String phoneNumber,
      String address) {
    if (status == KycStatus.LOCKED) {
      throw new BusinessRuleException("REJECTION_LIMIT_REACHED", "KYC application is locked after repeated rejection.");
    }
    this.legalName = legalName.trim();
    this.dateOfBirth = dateOfBirth;
    this.nationalIdentityHash = sha256(nationalIdentityNumber.trim());
    this.phoneNumber = phoneNumber.trim();
    this.address = address.trim();
    this.status = KycStatus.DRAFT;
    this.updatedAt = Instant.now();
  }

  public void submitForReview(boolean hasIdentityDocument, boolean hasSelfie) {
    if (status != KycStatus.DRAFT && status != KycStatus.RESUBMISSION_REQUIRED && status != KycStatus.REJECTED) {
      throw new BusinessRuleException("KYC_NOT_EDITABLE", "Only draft or resubmission KYC applications can be submitted.");
    }
    if (!hasIdentityDocument || !hasSelfie) {
      throw new BusinessRuleException("KYC_EVIDENCE_REQUIRED", "Identity document and selfie evidence are required before KYC submission.");
    }
    this.status = KycStatus.PENDING_REVIEW;
    this.updatedAt = Instant.now();
  }

  public KycStatus approve(String reviewedBy, String reason) {
    requirePendingReview();
    var before = this.status;
    review(KycStatus.APPROVED, reviewedBy, reason);
    return before;
  }

  public KycStatus reject(String reviewedBy, String reason) {
    requirePendingReview();
    var before = this.status;
    this.rejectionCount += 1;
    review(this.rejectionCount >= 3 ? KycStatus.LOCKED : KycStatus.REJECTED, reviewedBy, reason);
    return before;
  }

  public KycStatus requestResubmission(String reviewedBy, String reason) {
    requirePendingReview();
    var before = this.status;
    review(KycStatus.RESUBMISSION_REQUIRED, reviewedBy, reason);
    return before;
  }

  private void requirePendingReview() {
    if (status != KycStatus.PENDING_REVIEW) {
      throw new BusinessRuleException("KYC_NOT_PENDING_REVIEW", "Only pending KYC applications can receive an admin decision.");
    }
  }

  private void review(KycStatus nextStatus, String actor, String reason) {
    this.status = nextStatus;
    this.reviewedBy = actor;
    this.reviewReason = reason.trim();
    this.reviewedAt = Instant.now();
    this.updatedAt = this.reviewedAt;
  }

  private static String sha256(String input) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  public String publicId() { return publicId; }
  public String keycloakSubject() { return keycloakSubject; }
  public String legalName() { return legalName; }
  public LocalDate dateOfBirth() { return dateOfBirth; }
  public String phoneNumber() { return phoneNumber; }
  public String address() { return address; }
  public KycStatus status() { return status; }
  public int rejectionCount() { return rejectionCount; }
  public Instant reviewedAt() { return reviewedAt; }
  public String reviewedBy() { return reviewedBy; }
  public String reviewReason() { return reviewReason; }
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
}
