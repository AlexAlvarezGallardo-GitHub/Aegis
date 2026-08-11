package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.TransferAuditRecordService;
import com.aegis.audit.domain.event.TransferCompletedEvent;
import com.aegis.audit.domain.model.TransferAuditRecord;
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
 * Kafka consumer that listens for {@code payment.transfer.completed} events.
 * <p>
 * Each incoming event is mapped to a {@link TransferAuditRecord} and persisted
 * to provide a durable audit trail. Event identifiers are recorded in the
 * {@code processed_events} table so that at-least-once delivery does not
 * produce duplicate audit records.</p>
 */
@Component
public class TransferCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferCompletedConsumer.class);

    private final TransferAuditRecordService transferAuditRecordService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public TransferCompletedConsumer(TransferAuditRecordService transferAuditRecordService,
                                      ProcessedEventJpaRepository processedEventRepository) {
        this.transferAuditRecordService = transferAuditRecordService;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Consumes a {@link TransferCompletedEvent} from Kafka, maps it to a
     * {@link TransferAuditRecord}, and persists it to the database.
     *
     * @param event     the deserialized transfer completed event
     * @param topic     the source topic
     * @param partition the source partition
     * @param offset    the source offset
     */
    @KafkaListener(
            topics = "${aegis.kafka.topics.transfer-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "transferCompletedListenerContainerFactory"
    )
    @Transactional
    public void consume(TransferCompletedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received TransferCompletedEvent: eventId={}, transferId={}, amount={} {}",
                event.eventId(), event.transferId(), event.amount(), event.currency());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        TransferAuditRecord record = TransferAuditRecord.create(
                event.eventId(),
                event.transferId(),
                "COMPLETED",
                event.sourceWalletId(),
                event.destWalletId(),
                event.userId(),
                event.amount(),
                event.currency(),
                event.reference(),
                null,
                event.correlationId(),
                event.timestamp(),
                Instant.now()
        );

        transferAuditRecordService.save(record);

        log.info("TransferAuditRecord persisted: id={}, transferId={}", record.id(), record.transferId());
    }
}
