package com.finflow.user.infrastructure.persistence;

import com.finflow.user.domain.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
  Optional<UserProfile> findByKeycloakSubject(String keycloakSubject);
}
