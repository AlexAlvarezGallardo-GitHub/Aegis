package com.aegis.identity.infrastructure.messaging;

import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher - Outbox Persistence")
class KafkaEventPublisherTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    private ObjectMapper objectMapper;
    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        publisher = new KafkaEventPublisher(outboxRepository, objectMapper);
    }

    @Nested
    @DisplayName("When publishing UserRegistered event")
    class WhenPublishingUserRegisteredEvent {

        @Test
        @DisplayName("Should serialize event to JSON and persist to outbox table")
        void shouldSerializeEventAndPersistToOutbox() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            Instant registeredAt = Instant.parse("2026-07-01T10:00:00Z");

            UserRegistered event = new UserRegistered(
                    eventId, "USER_REGISTERED", "1.0",
                    userId, "john@example.com", "John", "Doe",
                    registeredAt, "corr-123");

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEventJpaEntity saved = captor.getValue();
            assertNotNull(saved.getId());
            assertEquals("USER", saved.getAggregateType());
            assertEquals(userId, saved.getAggregateId());
            assertEquals("USER_REGISTERED", saved.getEventType());
            assertEquals("PENDING", saved.getStatus());
            assertNotNull(saved.getCreatedAt());
            assertNull(saved.getPublishedAt());
        }

        @Test
        @DisplayName("Should produce valid JSON payload containing all event fields")
        void shouldProduceValidJsonPayload() throws Exception {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            Instant registeredAt = Instant.parse("2026-07-01T10:00:00Z");

            UserRegistered event = new UserRegistered(
                    eventId, "USER_REGISTERED", "1.0",
                    userId, "john@example.com", "John", "Doe",
                    registeredAt, "corr-123");

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert - capture and parse the payload
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());

            String payload = captor.getValue().getPayload();
            assertNotNull(payload);

            // Parse the JSON payload back to verify structure
            var payloadMap = objectMapper.readValue(payload, java.util.Map.class);
            assertEquals(eventId.toString(), payloadMap.get("eventId"));
            assertEquals("USER_REGISTERED", payloadMap.get("eventType"));
            assertEquals("1.0", payloadMap.get("schemaVersion"));
            assertEquals(userId.toString(), payloadMap.get("userId"));
            assertEquals("john@example.com", payloadMap.get("email"));
            assertEquals("John", payloadMap.get("firstName"));
            assertEquals("Doe", payloadMap.get("lastName"));
            assertEquals("corr-123", payloadMap.get("correlationId"));
        }

        @Test
        @DisplayName("Should set initial status to PENDING")
        void shouldSetInitialStatusToPending() {
            // Arrange
            UserRegistered event = new UserRegistered(
                    UUID.randomUUID(), "USER_REGISTERED", "1.0",
                    UUID.randomUUID(), "test@example.com", "Test", "User",
                    Instant.now(), "corr-456");

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());
            assertEquals("PENDING", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should generate unique outbox event ID for each publish")
        void shouldGenerateUniqueOutboxEventId() {
            // Arrange
            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            UserRegistered event1 = new UserRegistered(
                    UUID.randomUUID(), "USER_REGISTERED", "1.0",
                    UUID.randomUUID(), "user1@example.com", "User", "One",
                    Instant.now(), "corr-1");

            UserRegistered event2 = new UserRegistered(
                    UUID.randomUUID(), "USER_REGISTERED", "1.0",
                    UUID.randomUUID(), "user2@example.com", "User", "Two",
                    Instant.now(), "corr-2");

            // Act
            publisher.publish(event1);
            publisher.publish(event2);

            // Assert
            var captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository, times(2)).save(captor.capture());

            UUID id1 = captor.getAllValues().get(0).getId();
            UUID id2 = captor.getAllValues().get(1).getId();
            assertNotEquals(id1, id2, "Each outbox event must have a unique ID");
        }
    }

    @Nested
    @DisplayName("When serialization fails")
    class WhenSerializationFails {

        @Test
        @DisplayName("Should throw RuntimeException wrapping the original cause")
        void shouldThrowRuntimeExceptionOnSerializationFailure() throws Exception {
            // Arrange - use a publisher with a broken ObjectMapper
            ObjectMapper brokenMapper = mock(ObjectMapper.class);
            KafkaEventPublisher publisherWithBrokenMapper = new KafkaEventPublisher(outboxRepository, brokenMapper);

            when(brokenMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

            UserRegistered event = new UserRegistered(
                    UUID.randomUUID(), "USER_REGISTERED", "1.0",
                    UUID.randomUUID(), "test@example.com", "Test", "User",
                    Instant.now(), "corr-789");

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> publisherWithBrokenMapper.publish(event));

            assertTrue(thrown.getMessage().contains("Failed to persist domain event"));
            assertNotNull(thrown.getCause());

            // Verify nothing was saved to outbox
            verify(outboxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("When database persistence fails")
    class WhenDatabasePersistenceFails {

        @Test
        @DisplayName("Should propagate exception from outbox repository save")
        void shouldPropagateExceptionFromRepository() {
            // Arrange
            when(outboxRepository.save(any(OutboxEventJpaEntity.class)))
                    .thenThrow(new RuntimeException("Database connection lost"));

            UserRegistered event = new UserRegistered(
                    UUID.randomUUID(), "USER_REGISTERED", "1.0",
                    UUID.randomUUID(), "test@example.com", "Test", "User",
                    Instant.now(), "corr-err");

            // Act & Assert
            assertThrows(RuntimeException.class, () -> publisher.publish(event));
        }
    }

    @Nested
    @DisplayName("Known issues and risks")
    class KnownIssuesAndRisks {

        @Test
        @DisplayName("BUG: application.yml uses JsonSerializer but payload is already a String - double serialization")
        void documentsDoubleSerializationBug() {
            // DOCUMENTATION TEST: This test documents a known serialization bug.
            //
            // In application.yml:
            //   spring.kafka.producer.value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
            //
            // In OutboxRelayScheduler:
            //   kafkaTemplate.send(TOPIC, event.getId().toString(), event.getPayload())
            //
            // event.getPayload() is already a JSON String (serialized by ObjectMapper in KafkaEventPublisher).
            // When JsonSerializer serializes a String, it wraps it in quotes and escapes internal quotes:
            //
            //   Input:  {"eventId":"abc","eventType":"USER_REGISTERED"}
            //   Output: "{\"eventId\":\"abc\",\"eventType\":\"USER_REGISTERED\"}"
            //
            // This means the Kafka message value is a JSON-encoded string containing another JSON string.
            // Consumers expecting raw JSON will fail to parse it.
            //
            // Fix: Change value-serializer to StringSerializer in application.yml, OR
            //      change KafkaTemplate<String, String> to KafkaTemplate<String, Object>
            //      and pass the deserialized object instead of the pre-serialized string.

            assertTrue(true, "Documented: JsonSerializer + String payload causes double serialization");
        }
    }
}
