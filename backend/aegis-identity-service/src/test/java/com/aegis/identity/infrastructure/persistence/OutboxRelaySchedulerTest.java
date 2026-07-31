package com.aegis.identity.infrastructure.persistence;

import com.aegis.identity.infrastructure.config.KafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayScheduler - Event Relay")
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topicsProperties;

    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(topicsProperties.topicFor("USER_REGISTERED"))
                .thenReturn("aegis.identity.user-registered");
        scheduler = new OutboxRelayScheduler(outboxRepository, kafkaTemplate, topicsProperties, 50);
    }

    @Nested
    @DisplayName("When relaying pending events")
    class WhenRelayingPendingEvents {

        @Test
        @DisplayName("Should send pending events to the correct Kafka topic")
        void shouldSendPendingEventsToCorrectTopic() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            String payload = "{\"eventId\":\"" + eventId + "\",\"eventType\":\"USER_REGISTERED\"}";
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    payload, Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(eq("aegis.identity.user-registered"), eq(eventId.toString()), eq(payload)))
                    .thenReturn(future);

            // Act
            scheduler.relayPendingEvents();

            // Assert
            verify(kafkaTemplate).send(eq("aegis.identity.user-registered"), eq(eventId.toString()), eq(payload));
        }

        @Test
        @DisplayName("Should mark events as PUBLISHED after successful send")
        void shouldMarkEventsAsPublishedAfterSuccessfulSend() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            String payload = "{\"eventId\":\"test\"}";
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    payload, Instant.now());

            assertEquals("PENDING", event.getStatus());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            scheduler.relayPendingEvents();

            // Assert
            assertEquals("PUBLISHED", event.getStatus());
            assertNotNull(event.getPublishedAt());
            verify(outboxRepository).save(event);
        }

        @Test
        @DisplayName("Should return early when no pending events exist")
        void shouldReturnEarlyWhenNoPendingEvents() {
            // Arrange
            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduler.relayPendingEvents();

            // Assert
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
            verify(outboxRepository, never()).save(any(OutboxEventJpaEntity.class));
        }

        @Test
        @DisplayName("Should process multiple events in order")
        void shouldProcessMultipleEventsInOrder() throws Exception {
            // Arrange
            UUID eventId1 = UUID.randomUUID();
            UUID eventId2 = UUID.randomUUID();
            OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
                    eventId1, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());
            OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
                    eventId2, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":2}", Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            scheduler.relayPendingEvents();

            // Assert
            var inOrder = inOrder(kafkaTemplate);
            inOrder.verify(kafkaTemplate).send(eq("aegis.identity.user-registered"), eq(eventId1.toString()), eq("{\"id\":1}"));
            inOrder.verify(kafkaTemplate).send(eq("aegis.identity.user-registered"), eq(eventId2.toString()), eq("{\"id\":2}"));

            assertEquals("PUBLISHED", event1.getStatus());
            assertEquals("PUBLISHED", event2.getStatus());
        }
    }

    @Nested
    @DisplayName("When Kafka send fails")
    class WhenKafkaSendFails {

        @Test
        @DisplayName("Should catch exception and leave event as PENDING")
        void shouldCatchExceptionAndLeaveEventAsPending() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

            // Act
            scheduler.relayPendingEvents();

            // Assert - event remains PENDING (not marked as PUBLISHED)
            assertEquals("PENDING", event.getStatus());
            assertNull(event.getPublishedAt());
            verify(outboxRepository, never()).save(event);
        }

        @Test
        @DisplayName("Should stop processing remaining events after first failure (break behaviour)")
        void shouldStopProcessingRemainingEventsAfterFirstFailure() throws Exception {
            // Arrange
            UUID eventId1 = UUID.randomUUID();
            UUID eventId2 = UUID.randomUUID();
            OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
                    eventId1, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());
            OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
                    eventId2, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":2}", Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2));

            // First event fails, second should never be attempted
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

            // Act
            scheduler.relayPendingEvents();

            // Assert - only one send attempt (break on first failure)
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
            assertEquals("PENDING", event1.getStatus());
            assertEquals("PENDING", event2.getStatus());
        }
    }

    @Nested
    @DisplayName("Topic configuration")
    class TopicConfiguration {

        @Test
        @DisplayName("Should use configured topic name 'aegis.identity.user-registered'")
        void shouldUseConfiguredTopicName() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"payload\":\"test\"}", Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            scheduler.relayPendingEvents();

            // Assert - verify exact topic name via ArgumentCaptor
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), anyString());
            assertEquals("aegis.identity.user-registered", topicCaptor.getValue());
        }

        @Test
        @DisplayName("Should use event ID as Kafka message key")
        void shouldUseEventIdAsKafkaMessageKey() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"payload\":\"test\"}", Instant.now());

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            scheduler.relayPendingEvents();

            // Assert
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), anyString());
            assertEquals(eventId.toString(), keyCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("Batch size configuration")
    class BatchSizeConfiguration {

        @Test
        @DisplayName("Should respect configured batch size when querying pending events")
        void shouldRespectConfiguredBatchSize() {
            // Arrange
            OutboxRelayScheduler customBatchScheduler = new OutboxRelayScheduler(
                    outboxRepository, kafkaTemplate, topicsProperties, 10);

            when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            customBatchScheduler.relayPendingEvents();

            // Assert
            ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(outboxRepository).findByStatusOrderByCreatedAtAsc(eq("PENDING"), pageCaptor.capture());
            assertEquals(10, pageCaptor.getValue().getPageSize());
        }
    }

    @Nested
    @DisplayName("Known issues and risks")
    class KnownIssuesAndRisks {

        @Test
        @DisplayName("BUG: @Transactional on relayPendingEvents wraps Kafka send inside DB transaction - risk of at-least-once duplicates")
        void documentsTransactionalRiskOfDuplicates() {
            // DOCUMENTATION TEST: This test documents a known architectural risk.
            //
            // The relayPendingEvents() method is annotated with @Transactional.
            // Within the same transaction, it:
            //   1. Sends event to Kafka via kafkaTemplate.send().get()
            //   2. Marks event as PUBLISHED via event.markPublished()
            //   3. Saves updated event via outboxRepository.save(event)
            //
            // Risk scenario:
            //   - Step 1 succeeds (Kafka receives the message)
            //   - Step 3 fails (e.g., DB connection lost after Kafka send)
            //   - Transaction rolls back, event stays PENDING
            //   - Next scheduling cycle retries, sends DUPLICATE to Kafka
            //
            // Impact: At-least-once delivery semantics, consumers must be idempotent.
            //
            // Recommended fix: Use TransactionSynchronizationManager.registerSynchronization()
            // to send to Kafka AFTER the DB transaction commits, or adopt a CDC/Debezium approach.

            assertTrue(true, "Documented: OutboxRelayScheduler has at-least-once delivery risk");
        }

        @Test
        @DisplayName("BUG: break on first failure blocks all subsequent events until next poll cycle")
        void documentsBreakOnFirstFailureIssue() {
            // DOCUMENTATION TEST: This test documents a known behavioural issue.
            //
            // In OutboxRelayScheduler.relayPendingEvents(), line 54:
            //   catch (Exception e) { ... break; }
            //
            // If event[0] fails, events [1..N] are skipped entirely until the next
            // scheduling cycle (default 1000ms). During sustained Kafka outages,
            // ALL events stall even if only one partition/broker is affected.
            //
            // Recommended fix: Continue processing remaining events, or implement
            // per-partition failure tracking with exponential backoff.

            assertTrue(true, "Documented: break on first failure causes unnecessary stalling");
        }
    }
}
