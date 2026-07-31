package com.aegis.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Finds PENDING outbox events with a pessimistic write lock and SKIP LOCKED
     * to prevent race conditions in multi-instance deployments.
     *
     * <p>Uses {@code FOR UPDATE SKIP LOCKED} so that concurrent scheduler instances
     * do not block each other: each instance picks a non-overlapping set of rows.</p>
     *
     * @param status   the event status to filter by (typically "PENDING")
     * @param pageable the pagination constraints (controls batch size)
     * @return the locked list of matching outbox events
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEventJpaEntity e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<OutboxEventJpaEntity> findPendingEventsForProcessing(String status, Pageable pageable);
}
