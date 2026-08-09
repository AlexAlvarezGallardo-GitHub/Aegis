package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.infrastructure.config.KafkaTopicsProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Resilience: the outbox relay must not lose events when Kafka is unavailable.
 * A failed send leaves the event PENDING so it is retried on the next poll
 * (at-least-once delivery).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox resilience - Kafka unavailable")
class OutboxRelaySchedulerResilienceTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaTopicsProperties topics;
    private OutboxRelayScheduler scheduler;

    private void setUpScheduler(int batchSize) {
        topics = new KafkaTopicsProperties();
        topics.setTopics(java.util.Map.of("FUNDS_DEPOSITED", "wallet.funds.deposited"));
        scheduler = new OutboxRelayScheduler(
                outboxRepository, kafkaTemplate, topics, new SimpleMeterRegistry(), batchSize);
    }

    @Nested
    @DisplayName("When Kafka send fails")
    class WhenKafkaSendFails {

        @Test
        @DisplayName("Should keep the event PENDING so it is retried later")
        void shouldKeepEventPendingWhenSendFails() {
            setUpScheduler(50);
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "WALLET", UUID.randomUUID(),
                    "FUNDS_DEPOSITED", "{\"amount\":100}", Instant.now());

            when(outboxRepository.countByStatus("PENDING")).thenReturn(1L);
            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(Pageable.class)))
                    .thenReturn(List.of(event));
            // Kafka unavailable: send future completes exceptionally
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));

            scheduler.relayPendingEvents();

            // The event must NOT be marked published
            assertEquals("PENDING", event.getStatus(), "event must stay PENDING after a failed send");
            assertNull(event.getPublishedAt());
            verify(outboxRepository, never()).save(argThat(e -> "PUBLISHED".equals(e.getStatus())));
        }
    }

    @Nested
    @DisplayName("When Kafka send succeeds")
    class WhenKafkaSendSucceeds {

        @Test
        @DisplayName("Should mark the event PUBLISHED")
        void shouldMarkEventPublished() throws Exception {
            setUpScheduler(50);
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "WALLET", UUID.randomUUID(),
                    "FUNDS_DEPOSITED", "{\"amount\":100}", Instant.now());

            when(outboxRepository.countByStatus("PENDING")).thenReturn(1L);
            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(Pageable.class)))
                    .thenReturn(List.of(event));
            @SuppressWarnings("unchecked")
            SendResult<String, String> result = mock(SendResult.class);
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(result));

            scheduler.relayPendingEvents();

            assertEquals("PUBLISHED", event.getStatus());
            assertNotNull(event.getPublishedAt());
            verify(outboxRepository).save(event);
        }
    }

    @Nested
    @DisplayName("When topic mapping is missing")
    class WhenTopicMissing {

        @Test
        @DisplayName("Should not crash and should mark the event published with a warning")
        void shouldHandleMissingTopicWithoutCrashing() {
            setUpScheduler(50);
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    UUID.randomUUID(), "WALLET", UUID.randomUUID(),
                    "UNKNOWN_EVENT", "{}", Instant.now());

            when(outboxRepository.countByStatus("PENDING")).thenReturn(1L);
            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(Pageable.class)))
                    .thenReturn(List.of(event));

            scheduler.relayPendingEvents();

            assertEquals("PUBLISHED", event.getStatus(), "missing topic mapping is skipped, not retried forever");
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }
}
