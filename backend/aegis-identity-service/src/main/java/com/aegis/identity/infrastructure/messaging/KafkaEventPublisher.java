package com.aegis.identity.infrastructure.messaging;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(OutboxEventJpaRepository outboxRepository,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(UserRegistered event) {
        publish("USER", event.userId(), event.eventType(), event);
    }

    @Override
    public void publish(UserAuthenticated event) {
        publish("USER", event.userId(), event.eventType(), event);
    }

    @Override
    public void publish(UserAccountLocked event) {
        publish("USER", event.userId(), event.eventType(), event);
    }

    private void publish(String aggregateType, UUID aggregateId, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    UuidV7Generator.generate(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    payload,
                    Instant.now()
            );
            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event: aggregateId={}, eventType={}", aggregateId, eventType);
        } catch (Exception e) {
            log.error("Failed to persist outbox event for aggregateId={}", aggregateId, e);
            throw new RuntimeException("Failed to persist domain event", e);
        }
    }
}
