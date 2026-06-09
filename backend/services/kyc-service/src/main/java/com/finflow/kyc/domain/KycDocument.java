package com.finflow.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_documents")
public class KycDocument {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String publicId;

  @Column(nullable = false)
  private String applicationPublicId;

  @Column(nullable = false)
  private String keycloakSubject;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KycDocumentType documentType;

  @Column(nullable = false)
  private String bucket;

  @Column(nullable = false)
  private String objectKey;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false)
  private String checksum;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KycDocumentStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected KycDocument() {
  }

  public static KycDocument requested(
      String applicationPublicId,
      String keycloakSubject,
      KycDocumentType documentType,
      String bucket,
      String objectKey,
      String contentType,
      long sizeBytes,
      String checksum) {
    var now = Instant.now();
    var document = new KycDocument();
    document.id = UUID.randomUUID();
    document.publicId = PublicIds.next("doc");
    document.applicationPublicId = applicationPublicId;
    document.keycloakSubject = keycloakSubject;
    document.documentType = documentType;
    document.bucket = bucket;
    document.objectKey = objectKey;
    document.contentType = contentType;
    document.sizeBytes = sizeBytes;
    document.checksum = checksum;
    document.status = KycDocumentStatus.UPLOAD_REQUESTED;
    document.createdAt = now;
    document.updatedAt = now;
    return document;
  }

  public void confirmUploaded(String checksum) {
    if (!this.checksum.equals(checksum)) {
      throw new BusinessRuleException("KYC_DOCUMENT_CHECKSUM_MISMATCH", "Uploaded document checksum does not match the upload session.");
    }
    this.status = KycDocumentStatus.UPLOADED;
    this.updatedAt = Instant.now();
  }

  public String publicId() { return publicId; }
  public String applicationPublicId() { return applicationPublicId; }
  public String keycloakSubject() { return keycloakSubject; }
  public KycDocumentType documentType() { return documentType; }
  public String bucket() { return bucket; }
  public String objectKey() { return objectKey; }
  public String contentType() { return contentType; }
  public long sizeBytes() { return sizeBytes; }
  public String checksum() { return checksum; }
  public KycDocumentStatus status() { return status; }
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
}
