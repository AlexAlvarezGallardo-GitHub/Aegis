package com.aegis.identity.infrastructure.messaging;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    UuidV7Generator.generate(),
                    "USER",
                    event.userId(),
                    event.eventType(),
                    payload,
                    Instant.now()
            );
            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event: userId={}, eventType={}", event.userId(), event.eventType());
        } catch (Exception e) {
            log.error("Failed to persist outbox event for userId={}", event.userId(), e);
            throw new RuntimeException("Failed to persist domain event", e);
        }
    }
}
