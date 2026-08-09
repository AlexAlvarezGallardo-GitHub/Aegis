package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.infrastructure.config.KafkaTopicsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private static final long KAFKA_SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;
    private final int batchSize;
    private final AtomicLong pendingEvents = new AtomicLong();

    public OutboxRelayScheduler(OutboxEventJpaRepository outboxRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 KafkaTopicsProperties topicsProperties,
                                 MeterRegistry meterRegistry,
                                 @Value("${aegis.wallet.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicsProperties = topicsProperties;
        this.batchSize = batchSize;
        Gauge.builder("aegis.outbox.pending_events", pendingEvents, AtomicLong::get)
                .description("Number of outbox events not yet published to Kafka")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${aegis.wallet.outbox.polling-interval-ms:1000}")
    @Transactional
    public void relayPendingEvents() {
        long count = outboxRepository.countByStatus("PENDING");
        pendingEvents.set(count);

        List<OutboxEventJpaEntity> pending = outboxRepository
                .findPendingEventsForProcessing("PENDING", PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                String topic = topicsProperties.topicFor(event.getEventType());
                if (topic == null) {
                    log.warn("No topic configured for event type: {} (check aegis.kafka.topics)", event.getEventType());
                    event.markPublished();
                    outboxRepository.save(event);
                    continue;
                }
                kafkaTemplate.send(topic, event.getId().toString(), event.getPayload())
                        .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
