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
@Table(name = "kyc_decisions")
public class KycDecision {
  @Id
  private UUID id;

  @Column(nullable = false)
  private String applicationPublicId;

  @Column(nullable = false)
  private String keycloakSubject;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KycStatus beforeStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KycStatus afterStatus;

  @Column(nullable = false)
  private String decidedBy;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false)
  private Instant createdAt;

  protected KycDecision() {
  }

  public static KycDecision record(KycApplication application, KycStatus beforeStatus, String decidedBy, String reason) {
    var decision = new KycDecision();
    decision.id = UUID.randomUUID();
    decision.applicationPublicId = application.publicId();
    decision.keycloakSubject = application.keycloakSubject();
    decision.beforeStatus = beforeStatus;
    decision.afterStatus = application.status();
    decision.decidedBy = decidedBy;
    decision.reason = reason.trim();
    decision.createdAt = Instant.now();
    return decision;
  }
}
