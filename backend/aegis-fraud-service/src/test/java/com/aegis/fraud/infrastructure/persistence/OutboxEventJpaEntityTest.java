package com.aegis.fraud.infrastructure.persistence;

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
                    id, "FRAUD_ASSESSMENT", aggregateId, "FRAUD_ASSESSMENT_COMPLETED",
                    "{\"test\":\"payload\"}", createdAt);

            assertEquals(id, entity.getId());
            assertEquals("FRAUD_ASSESSMENT", entity.getAggregateType());
            assertEquals(aggregateId, entity.getAggregateId());
            assertEquals("FRAUD_ASSESSMENT_COMPLETED", entity.getEventType());
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
                    UUID.randomUUID(), "FRAUD_ASSESSMENT", UUID.randomUUID(),
                    "FRAUD_ASSESSMENT_COMPLETED", "{}", Instant.now());

            assertEquals("PENDING", entity.getStatus());
            assertNull(entity.getPublishedAt());

            Instant beforeMark = Instant.now();
            entity.markPublished();
            Instant afterMark = Instant.now();

            assertEquals("PUBLISHED", entity.getStatus());
            assertNotNull(entity.getPublishedAt());
            assertTrue(entity.getPublishedAt().compareTo(beforeMark) >= 0);
            assertTrue(entity.getPublishedAt().compareTo(afterMark) <= 0);
        }

        @Test
        @DisplayName("Should allow marking multiple times (idempotent)")
        void shouldAllowMarkingMultipleTimes() {
            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    UUID.randomUUID(), "FRAUD_ASSESSMENT", UUID.randomUUID(),
                    "FRAUD_ASSESSMENT_COMPLETED", "{}", Instant.now());

            entity.markPublished();
            Instant firstPublishedAt = entity.getPublishedAt();

            entity.markPublished();
            Instant secondPublishedAt = entity.getPublishedAt();

            assertEquals("PUBLISHED", entity.getStatus());
            assertNotNull(secondPublishedAt);
            assertTrue(secondPublishedAt.compareTo(firstPublishedAt) >= 0);
        }
    }
}
