package com.aegis.identity.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for outbox event entities.
 */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Finds outbox events with the given status ordered by creation timestamp.
     *
     * @param status   the event status to filter by
     * @param pageable the pagination constraints
     * @return the list of matching outbox events
     */
    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
