package com.aegis.identity.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private static final Map<String, String> TOPIC_MAP = Map.of(
            "USER_REGISTERED", "aegis.identity.user-registered",
            "USER_AUTHENTICATED", "aegis.identity.user-authenticated",
            "USER_ACCOUNT_LOCKED", "aegis.identity.user-account-locked"
    );

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxRelayScheduler(OutboxEventJpaRepository outboxRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 @Value("${aegis.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${aegis.outbox.polling-interval-ms:1000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pending = outboxRepository
                .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                String topic = TOPIC_MAP.get(event.getEventType());
                if (topic == null) {
                    log.warn("No topic mapping for event type: {}", event.getEventType());
                    event.markPublished();
                    outboxRepository.save(event);
                    continue;
                }
                kafkaTemplate.send(topic, event.getId().toString(), event.getPayload()).get();
                event.markPublished();
                outboxRepository.save(event);
                log.debug("Published outbox event: id={}, type={}, topic={}", event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
                break;
            }
        }
    }
}
