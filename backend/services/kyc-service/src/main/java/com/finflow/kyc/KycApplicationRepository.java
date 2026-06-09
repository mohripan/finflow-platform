package com.finflow.kyc;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface KycApplicationRepository extends JpaRepository<KycApplication, UUID> {
  Optional<KycApplication> findByKeycloakSubject(String keycloakSubject);
}
