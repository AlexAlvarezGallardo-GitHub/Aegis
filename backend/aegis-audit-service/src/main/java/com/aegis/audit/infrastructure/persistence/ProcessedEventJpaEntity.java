package com.aegis.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks which Kafka events this consumer has already processed so that
 * at-least-once delivery does not produce duplicate audit records.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "partition", nullable = false)
    private int partition;

    @Column(name = "offset", nullable = false)
    private long offset;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventJpaEntity() {
    }

    public ProcessedEventJpaEntity(UUID eventId, String topic, int partition, long offset, Instant processedAt) {
        this.eventId = eventId;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.processedAt = processedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
