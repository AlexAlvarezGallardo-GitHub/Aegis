package com.aegis.reporting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TransferProjectionJpaEntity} entities.
 */
public interface TransferProjectionJpaRepository extends JpaRepository<TransferProjectionJpaEntity, UUID> {

    /**
     * Finds the transfer projection for the given transfer.
     *
     * @param transferId the transfer identifier
     * @return an optional containing the entity if found, or empty otherwise
     */
    Optional<TransferProjectionJpaEntity> findByTransferId(UUID transferId);
}
