package com.finflow.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
  Optional<UserProfile> findByKeycloakSubject(String keycloakSubject);
}
