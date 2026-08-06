package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.FraudAuditRecordService;
import com.aegis.audit.domain.event.FraudAssessmentCompletedEvent;
import com.aegis.audit.domain.model.FraudAuditRecord;
import com.aegis.audit.infrastructure.persistence.ProcessedEventJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Kafka consumer that listens for fraud assessment completed events
 * and persists them as fraud audit records. Event identifiers are recorded
 * in {@code processed_events} to make consumption idempotent.
 */
@Component
public class FraudAssessmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudAssessmentConsumer.class);

    private final FraudAuditRecordService fraudAuditRecordService;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public FraudAssessmentConsumer(FraudAuditRecordService fraudAuditRecordService,
                                    ProcessedEventJpaRepository processedEventRepository,
                                    ObjectMapper objectMapper) {
        this.fraudAuditRecordService = fraudAuditRecordService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${aegis.kafka.topics.fraud-assessment-completed}", groupId = "audit-group",
            containerFactory = "fraudAssessmentListenerContainerFactory")
    @Transactional
    public void consume(FraudAssessmentCompletedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received FraudAssessmentCompleted for assessment {} (decision {})",
                event.assessmentId(), event.decision());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(event.rulesEvaluated());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rule evaluations", e);
        }

        FraudAuditRecord record = FraudAuditRecord.create(
                event.assessmentId(),
                event.transactionId(),
                event.transactionType(),
                event.riskScore(),
                event.decision(),
                rulesJson,
                event.timestamp(),
                Instant.now()
        );

        fraudAuditRecordService.save(record);
        log.info("Persisted fraud audit record {} for transaction {}",
                record.id(), record.transactionId());
    }
}
