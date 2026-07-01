package com.aegis.identity.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);
    private static final String TOPIC = "aegis.identity.user-registered";

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public OutboxRelayScheduler(OutboxEventJpaRepository outboxRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${aegis.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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
                kafkaTemplate.send(TOPIC, event.getId().toString(), event.getPayload()).get();
                event.markPublished();
                outboxRepository.save(event);
                log.debug("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
                break;
            }
        }
    }
}
