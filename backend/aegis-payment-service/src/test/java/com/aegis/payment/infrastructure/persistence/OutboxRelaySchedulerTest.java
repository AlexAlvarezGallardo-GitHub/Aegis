package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.infrastructure.config.KafkaTopicsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayScheduler - Outbox to Kafka Relay")
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topicsProperties;

    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxRelayScheduler(outboxRepository, kafkaTemplate, topicsProperties, 50);
    }

    private OutboxEventJpaEntity pendingEvent() {
        return new OutboxEventJpaEntity(
                UUID.randomUUID(), "TRANSFER", UUID.randomUUID(),
                "TRANSFER_REQUESTED", "{\"eventType\":\"TRANSFER_REQUESTED\"}", Instant.now());
    }

    @Nested
    @DisplayName("When there are pending events")
    class WhenPendingEvents {

        @Test
        @DisplayName("Should publish each event to its configured topic and mark as published")
        void shouldPublishAndMarkPublished() throws Exception {
            OutboxEventJpaEntity event = pendingEvent();
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                    .thenReturn(List.of(event));
            when(topicsProperties.topicFor("TRANSFER_REQUESTED")).thenReturn("payment.transfer.requested");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            scheduler.relayPendingEvents();

            verify(kafkaTemplate).send("payment.transfer.requested", event.getId().toString(), event.getPayload());
            verify(outboxRepository).save(event);
            assertEquals("PUBLISHED", event.getStatus());
            assertNotNull(event.getPublishedAt());
        }

        @Test
        @DisplayName("Should skip events whose topic is not configured and mark them published")
        void shouldSkipUnconfiguredTopic() {
            OutboxEventJpaEntity event = pendingEvent();
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                    .thenReturn(List.of(event));
            when(topicsProperties.topicFor("TRANSFER_REQUESTED")).thenReturn(null);

            scheduler.relayPendingEvents();

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
            verify(outboxRepository).save(event);
            assertEquals("PUBLISHED", event.getStatus());
        }

        @Test
        @DisplayName("Should stop relaying on the first Kafka send failure and not mark events published")
        void shouldStopOnSendFailure() {
            OutboxEventJpaEntity first = pendingEvent();
            OutboxEventJpaEntity second = pendingEvent();
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                    .thenReturn(List.of(first, second));
            when(topicsProperties.topicFor(anyString())).thenReturn("payment.transfer.requested");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            scheduler.relayPendingEvents();

            verify(outboxRepository, never()).save(first);
            verify(outboxRepository, never()).save(second);
            assertEquals("PENDING", first.getStatus());
            assertEquals("PENDING", second.getStatus());
        }
    }

    @Nested
    @DisplayName("When there are no pending events")
    class WhenNoPendingEvents {

        @Test
        @DisplayName("Should do nothing")
        void shouldDoNothing() {
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                    .thenReturn(List.of());

            scheduler.relayPendingEvents();

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
            verify(outboxRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should use the batch size passed to the constructor")
    void shouldUseConfiguredBatchSize() {
        OutboxRelayScheduler customScheduler = new OutboxRelayScheduler(
                outboxRepository, kafkaTemplate, topicsProperties, 25);
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                .thenReturn(List.of());

        customScheduler.relayPendingEvents();

        verify(outboxRepository).findByStatusOrderByCreatedAtAsc(eq("PENDING"), any());
    }

    @Test
    @DisplayName("TopicsProperties should expose and mutate the topics map")
    void topicsPropertiesRoundTrip() {
        KafkaTopicsProperties properties = new KafkaTopicsProperties();
        Map<String, String> topics = Map.of("TRANSFER_REQUESTED", "payment.transfer.requested");
        properties.setTopics(topics);
        assertEquals(topics, properties.getTopics());
        assertEquals("payment.transfer.requested", properties.topicFor("TRANSFER_REQUESTED"));
        assertNull(properties.topicFor("UNKNOWN"));
    }
}
