package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.domain.event.FraudAssessmentCompletedEvent;
import com.aegis.audit.domain.model.FraudAuditRecord;
import com.aegis.audit.infrastructure.persistence.FraudAuditRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class FraudAssessmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudAssessmentConsumer.class);

    private final FraudAuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    public FraudAssessmentConsumer(FraudAuditRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${aegis.kafka.topics.fraud-assessment-completed}", groupId = "audit-group",
            containerFactory = "fraudAssessmentListenerContainerFactory")
    public void consume(FraudAssessmentCompletedEvent event) {
        log.info("Received FraudAssessmentCompleted for assessment {} (decision {})",
                event.assessmentId(), event.decision());

        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(event.rulesEvaluated());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rule evaluations", e);
        }

        FraudAuditRecord record = new FraudAuditRecord(
                UUID.randomUUID(),
                event.assessmentId(),
                event.transactionId(),
                event.transactionType(),
                event.riskScore(),
                event.decision(),
                rulesJson,
                event.timestamp(),
                Instant.now());

        repository.save(record);
        log.info("Persisted fraud audit record {} for transaction {}",
                record.getId(), record.getTransactionId());
    }
}
