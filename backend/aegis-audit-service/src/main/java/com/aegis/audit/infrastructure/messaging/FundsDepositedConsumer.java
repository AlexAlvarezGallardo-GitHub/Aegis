package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.domain.event.FundsDepositedEvent;
import com.aegis.audit.domain.model.AuditRecord;
import com.aegis.audit.infrastructure.persistence.AuditRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer that listens for {@code wallet.funds.deposited} events.
 * <p>
 * Each incoming event is mapped to an {@link AuditRecord} and persisted
 * to provide a durable audit trail.
 * </p>
 */
@Component
public class FundsDepositedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FundsDepositedConsumer.class);

    private final AuditRecordRepository auditRecordRepository;

    public FundsDepositedConsumer(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    /**
     * Consumes a {@link FundsDepositedEvent} from Kafka, maps it to an {@link AuditRecord},
     * and persists it to the database.
     *
     * @param event the deserialized funds deposited event
     */
    @KafkaListener(
            topics = "${aegis.kafka.topics.funds-deposited}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(FundsDepositedEvent event) {
        log.info("Received FundsDepositedEvent: eventId={}, walletId={}, amount={} {}",
                event.eventId(), event.walletId(), event.amount(), event.currency());

        AuditRecord record = new AuditRecord(
                UUID.randomUUID(),
                event.walletId(),
                event.userId(),
                event.amount(),
                event.currency(),
                event.source(),
                event.reference(),
                event.newBalance(),
                event.timestamp(),
                Instant.now(),
                event.correlationId()
        );

        auditRecordRepository.save(record);

        log.info("AuditRecord persisted: id={}, walletId={}", record.getId(), record.getWalletId());
    }
}
