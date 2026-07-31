package com.aegis.identity.infrastructure.persistence;

import com.aegis.identity.infrastructure.config.KafkaTopicsProperties;
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
            UUID eventId = UUID.randomUUID();
            String payload = "{\"eventId\":\"" + eventId + "\",\"eventType\":\"USER_REGISTERED\"}";
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    payload, Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(eq("aegis.identity.user-registered"), eq(eventId.toString()), eq(payload)))
                    .thenReturn(future);

            scheduler.relayPendingEvents();

            verify(kafkaTemplate).send(eq("aegis.identity.user-registered"), eq(eventId.toString()), eq(payload));
        }

        @Test
        @DisplayName("Should mark events as PUBLISHED after successful send")
        void shouldMarkEventsAsPublishedAfterSuccessfulSend() throws Exception {
            UUID eventId = UUID.randomUUID();
            String payload = "{\"eventId\":\"test\"}";
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    payload, Instant.now());

            assertEquals("PENDING", event.getStatus());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            scheduler.relayPendingEvents();

            assertEquals("PUBLISHED", event.getStatus());
            assertNotNull(event.getPublishedAt());
            verify(outboxRepository).save(event);
        }

        @Test
        @DisplayName("Should return early when no pending events exist")
        void shouldReturnEarlyWhenNoPendingEvents() {
            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            scheduler.relayPendingEvents();

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
            verify(outboxRepository, never()).save(any(OutboxEventJpaEntity.class));
        }

        @Test
        @DisplayName("Should process multiple events in order")
        void shouldProcessMultipleEventsInOrder() throws Exception {
            UUID eventId1 = UUID.randomUUID();
            UUID eventId2 = UUID.randomUUID();
            OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
                    eventId1, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());
            OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
                    eventId2, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":2}", Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            scheduler.relayPendingEvents();

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
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

            scheduler.relayPendingEvents();

            assertEquals("PENDING", event.getStatus());
            assertNull(event.getPublishedAt());
            verify(outboxRepository, never()).save(event);
        }

        @Test
        @DisplayName("Should stop processing remaining events after first failure (break behaviour)")
        void shouldStopProcessingRemainingEventsAfterFirstFailure() throws Exception {
            UUID eventId1 = UUID.randomUUID();
            UUID eventId2 = UUID.randomUUID();
            OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
                    eventId1, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":1}", Instant.now());
            OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
                    eventId2, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"id\":2}", Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2));

            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

            scheduler.relayPendingEvents();

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
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"payload\":\"test\"}", Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            scheduler.relayPendingEvents();

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), anyString());
            assertEquals("aegis.identity.user-registered", topicCaptor.getValue());
        }

        @Test
        @DisplayName("Should use event ID as Kafka message key")
        void shouldUseEventIdAsKafkaMessageKey() throws Exception {
            UUID eventId = UUID.randomUUID();
            OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                    eventId, "USER", UUID.randomUUID(), "USER_REGISTERED",
                    "{\"payload\":\"test\"}", Instant.now());

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(List.of(event));

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            scheduler.relayPendingEvents();

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
            OutboxRelayScheduler customBatchScheduler = new OutboxRelayScheduler(
                    outboxRepository, kafkaTemplate, topicsProperties, 10);

            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            customBatchScheduler.relayPendingEvents();

            ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(outboxRepository).findPendingEventsForProcessing(eq("PENDING"), pageCaptor.capture());
            assertEquals(10, pageCaptor.getValue().getPageSize());
        }
    }

    @Nested
    @DisplayName("Pessimistic locking")
    class PessimisticLocking {

        @Test
        @DisplayName("Should use findPendingEventsForProcessing with pessimistic lock")
        void shouldUseLockedQuery() {
            when(outboxRepository.findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            scheduler.relayPendingEvents();

            verify(outboxRepository).findPendingEventsForProcessing(eq("PENDING"), any(PageRequest.class));
            verify(outboxRepository, never()).findByStatusOrderByCreatedAtAsc(anyString(), any());
        }
    }
}
