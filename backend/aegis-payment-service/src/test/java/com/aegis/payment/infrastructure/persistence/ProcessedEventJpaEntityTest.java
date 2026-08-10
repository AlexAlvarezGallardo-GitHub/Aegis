package com.aegis.payment.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedEventJpaEntity - Consumer Deduplication")
class ProcessedEventJpaEntityTest {

    @Test
    @DisplayName("Should store all deduplication metadata")
    void shouldStoreAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant processedAt = Instant.now();

        ProcessedEventJpaEntity entity = new ProcessedEventJpaEntity(
                eventId, "payment.transfer.completed", 3, 77L, processedAt);

        assertEquals(eventId, entity.getEventId());
        assertEquals("payment.transfer.completed", entity.getTopic());
        assertEquals(3, entity.getPartition());
        assertEquals(77L, entity.getOffset());
        assertEquals(processedAt, entity.getProcessedAt());
    }
}
