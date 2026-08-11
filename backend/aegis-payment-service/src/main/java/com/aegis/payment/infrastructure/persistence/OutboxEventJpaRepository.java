package com.aegis.payment.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for outbox event entities.
 */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Finds pending outbox events ordered by creation time, using pessimistic write locking
     * with {@code SKIP LOCKED} to support safe concurrent consumption by multiple instances.
     *
     * @param status   the event status to filter by
     * @param pageable pagination information
     * @return list of locked pending outbox events
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(@Param("status") String status, Pageable pageable);
}
