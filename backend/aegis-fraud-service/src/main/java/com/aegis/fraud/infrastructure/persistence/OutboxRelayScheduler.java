package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.infrastructure.config.KafkaTopicsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the transactional outbox for pending events and publishes them to Kafka.
 */
@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;
    private final int batchSize;

    public OutboxRelayScheduler(OutboxEventJpaRepository outboxRepository,
                                KafkaTemplate<String, String> kafkaTemplate,
                                KafkaTopicsProperties topicsProperties,
                                @Value("${aegis.fraud.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicsProperties = topicsProperties;
        this.batchSize = batchSize;
    }

    /**
     * Selects pending outbox events, sends them to Kafka, and marks them as published.
     */
    @Scheduled(fixedDelayString = "${aegis.fraud.outbox.polling-interval-ms:1000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pending = outboxRepository
                .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                String topic = topicsProperties.topicFor(event.getEventType());
                if (topic == null) {
                    log.warn("No topic configured for event type: {} (check aegis.kafka.topics)",
                            event.getEventType());
                    event.markPublished();
                    outboxRepository.save(event);
                    continue;
                }
                kafkaTemplate.send(topic, event.getId().toString(), event.getPayload()).get();
                event.markPublished();
                outboxRepository.save(event);
                log.debug("Published outbox event: id={}, type={}, topic={}",
                        event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
                break;
            }
        }
    }
}
