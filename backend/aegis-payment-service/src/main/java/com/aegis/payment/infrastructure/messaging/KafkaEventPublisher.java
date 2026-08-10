package com.aegis.payment.infrastructure.messaging;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.event.TransferCompleted;
import com.aegis.payment.domain.event.TransferFailed;
import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.payment.infrastructure.persistence.OutboxEventJpaRepository;
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
    public void publish(TransferRequested event) {
        persistOutboxEvent(event.aggregateType(), event.aggregateId(),
                event.eventType(), event);
    }

    @Override
    public void publish(TransferCompleted event) {
        persistOutboxEvent(event.aggregateType(), event.aggregateId(),
                event.eventType(), event);
    }

    @Override
    public void publish(TransferFailed event) {
        persistOutboxEvent(event.aggregateType(), event.aggregateId(),
                event.eventType(), event);
    }

    private void persistOutboxEvent(String aggregateType, java.util.UUID aggregateId,
                                    String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    UuidV7Generator.generate(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    json,
                    Instant.now()
            );
            outboxRepository.save(outboxEvent);
            log.debug("Saved outbox event: aggregateId={}, eventType={}",
                    aggregateId, eventType);
        } catch (Exception e) {
            log.error("Failed to persist outbox event for aggregateId={}",
                    aggregateId, e);
            throw new IllegalStateException("Failed to persist domain event", e);
        }
    }
}
