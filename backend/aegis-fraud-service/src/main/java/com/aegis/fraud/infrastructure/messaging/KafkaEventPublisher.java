package com.aegis.fraud.infrastructure.messaging;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.fraud.domain.event.FraudAssessmentCompleted;
import com.aegis.fraud.domain.port.outbound.EventPublisher;
import com.aegis.fraud.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.fraud.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Outbox-based event publisher. Persists domain events to the outbox table
 * within the same transaction as the aggregate, to be relayed to Kafka asynchronously.
 */
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
    public void publish(FraudAssessmentCompleted event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    UuidV7Generator.generate(),
                    "FRAUD_ASSESSMENT",
                    event.assessmentId(),
                    event.eventType(),
                    payload,
                    Instant.now()
            );
            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event: assessmentId={}, eventType={}",
                    event.assessmentId(), event.eventType());
        } catch (Exception e) {
            log.error("Failed to persist outbox event for assessmentId={}",
                    event.assessmentId(), e);
            throw new IllegalStateException("Failed to persist domain event", e);
        }
    }
}
