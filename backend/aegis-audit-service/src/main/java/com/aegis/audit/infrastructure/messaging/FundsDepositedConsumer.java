package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.AuditRecordService;
import com.aegis.audit.domain.event.FundsDepositedEvent;
import com.aegis.audit.domain.model.AuditRecord;
import com.aegis.audit.infrastructure.persistence.ProcessedEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Kafka consumer that listens for {@code wallet.funds.deposited} events.
 * <p>
 * Each incoming event is mapped to an {@link AuditRecord} and persisted
 * to provide a durable audit trail. Event identifiers are recorded in the
 * {@code processed_events} table so that at-least-once delivery does not
 * produce duplicate audit records.</p>
 */
@Component
public class FundsDepositedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FundsDepositedConsumer.class);

    private final AuditRecordService auditRecordService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public FundsDepositedConsumer(AuditRecordService auditRecordService,
                                  ProcessedEventJpaRepository processedEventRepository) {
        this.auditRecordService = auditRecordService;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Consumes a {@link FundsDepositedEvent} from Kafka, maps it to an
     * {@link AuditRecord}, and persists it to the database.
     *
     * @param event     the deserialized funds deposited event
     * @param topic     the source topic
     * @param partition the source partition
     * @param offset    the source offset
     */
    @KafkaListener(
            topics = "${aegis.kafka.topics.funds-deposited}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "fundsDepositedListenerContainerFactory"
    )
    @Transactional
    public void consume(FundsDepositedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received FundsDepositedEvent: eventId={}, walletId={}, amount={} {}",
                event.eventId(), event.walletId(), event.amount(), event.currency());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        AuditRecord record = AuditRecord.create(
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

        auditRecordService.save(record);

        log.info("AuditRecord persisted: id={}, walletId={}", record.id(), record.walletId());
    }
}
