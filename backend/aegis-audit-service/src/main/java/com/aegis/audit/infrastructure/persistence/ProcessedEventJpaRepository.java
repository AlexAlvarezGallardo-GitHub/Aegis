package com.aegis.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for {@link ProcessedEventJpaEntity}.
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, UUID> {

    /**
     * Atomically records an event as processed, ignoring the insert if the event
     * has already been processed (idempotent under at-least-once delivery).
     *
     * @param eventId     the unique event identifier
     * @param topic       the source topic
     * @param partition   the source partition
     * @param offset      the source offset
     * @param processedAt when the event was processed
     * @return the number of rows inserted (0 when the event was already seen)
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, topic, partition, offset, processed_at)
            VALUES (:eventId, :topic, :partition, :offset, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId,
                       @Param("topic") String topic,
                       @Param("partition") int partition,
                       @Param("offset") long offset,
                       @Param("processedAt") Instant processedAt);
}
