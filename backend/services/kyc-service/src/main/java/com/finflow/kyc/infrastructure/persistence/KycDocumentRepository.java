package com.finflow.kyc.infrastructure.persistence;

import com.finflow.kyc.domain.KycDocument;
import com.finflow.kyc.domain.KycDocumentStatus;
import com.finflow.kyc.domain.KycDocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {
  List<KycDocument> findByApplicationPublicIdOrderByCreatedAtAsc(String applicationPublicId);
  Optional<KycDocument> findByPublicIdAndApplicationPublicId(String publicId, String applicationPublicId);
  boolean existsByApplicationPublicIdAndDocumentTypeAndStatus(String applicationPublicId, KycDocumentType documentType, KycDocumentStatus status);
}
