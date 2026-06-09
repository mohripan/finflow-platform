package com.finflow.kyc.infrastructure.persistence;

import com.finflow.kyc.domain.KycDecision;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycDecisionRepository extends JpaRepository<KycDecision, UUID> {
}
