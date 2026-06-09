package com.finflow.kyc.infrastructure.persistence;

import com.finflow.kyc.domain.KycApplication;
import com.finflow.kyc.domain.KycStatus;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycApplicationRepository extends JpaRepository<KycApplication, UUID> {
  Optional<KycApplication> findByKeycloakSubject(String keycloakSubject);
  Optional<KycApplication> findByPublicId(String publicId);
  List<KycApplication> findByStatusOrderByUpdatedAtAsc(KycStatus status);
  List<KycApplication> findAllByOrderByUpdatedAtDesc();
}
