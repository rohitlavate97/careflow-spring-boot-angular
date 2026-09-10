package com.careflow.administration.repository;

import com.careflow.administration.domain.SystemMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for SystemMetadata.
 */
@Repository
public interface SystemMetadataRepository extends JpaRepository<SystemMetadata, String> {

    Optional<SystemMetadata> findByMetadataKey(String metadataKey);
}
