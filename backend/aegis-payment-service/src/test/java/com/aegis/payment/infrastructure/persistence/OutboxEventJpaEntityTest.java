package com.aegis.payment.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutboxEventJpaEntity - Domain Event Outbox")
class OutboxEventJpaEntityTest {

    @Nested
    @DisplayName("When creating a new outbox event")
    class WhenCreatingNewOutboxEvent {

        @Test
        @DisplayName("Should initialize with PENDING status")
        void shouldInitializeWithPendingStatus() {
            UUID id = UUID.randomUUID();
            UUID aggregateId = UUID.randomUUID();
            Instant createdAt = Instant.now();

            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    id, "TRANSFER", aggregateId, "TRANSFER_REQUESTED",
                    "{\"test\":\"payload\"}", createdAt);

            assertEquals(id, entity.getId());
            assertEquals("TRANSFER", entity.getAggregateType());
            assertEquals(aggregateId, entity.getAggregateId());
            assertEquals("TRANSFER_REQUESTED", entity.getEventType());
            assertEquals("{\"test\":\"payload\"}", entity.getPayload());
            assertEquals(createdAt, entity.getCreatedAt());
            assertEquals("PENDING", entity.getStatus());
            assertNull(entity.getPublishedAt());
        }
    }

    @Nested
    @DisplayName("When marking as published")
    class WhenMarkingAsPublished {

        @Test
        @DisplayName("Should change status to PUBLISHED and set publishedAt timestamp")
        void shouldChangeStatusAndSetTimestamp() {
            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    UUID.randomUUID(), "TRANSFER", UUID.randomUUID(),
                    "TRANSFER_REQUESTED", "{}", Instant.now());

            assertEquals("PENDING", entity.getStatus());
            assertNull(entity.getPublishedAt());

            entity.markPublished();

            assertEquals("PUBLISHED", entity.getStatus());
            assertNotNull(entity.getPublishedAt());
        }
    }
}
