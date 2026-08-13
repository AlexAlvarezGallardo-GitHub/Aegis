package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.RefundAuditRecordService;
import com.aegis.audit.domain.event.PaymentRefundedEvent;
import com.aegis.audit.domain.model.RefundAuditRecord;
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
 * Kafka consumer that listens for {@code payment.refunded} events.
 * <p>
 * Each incoming event is mapped to a {@link RefundAuditRecord} and persisted
 * to provide a durable audit trail. Event identifiers are recorded in the
 * {@code processed_events} table so that at-least-once delivery does not
 * produce duplicate audit records.</p>
 */
@Component
public class PaymentRefundedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundedConsumer.class);

    private final RefundAuditRecordService refundAuditRecordService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public PaymentRefundedConsumer(RefundAuditRecordService refundAuditRecordService,
                                    ProcessedEventJpaRepository processedEventRepository) {
        this.refundAuditRecordService = refundAuditRecordService;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Consumes a {@link PaymentRefundedEvent} from Kafka, maps it to a
     * {@link RefundAuditRecord}, and persists it to the database.
     *
     * @param event     the deserialized payment refunded event
     * @param topic     the source topic
     * @param partition the source partition
     * @param offset    the source offset
     */
    @KafkaListener(
            topics = "${aegis.kafka.topics.payment-refunded}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentRefundedListenerContainerFactory"
    )
    @Transactional
    public void consume(PaymentRefundedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received PaymentRefundedEvent: eventId={}, refundId={}, amount={} {}",
                event.eventId(), event.refundId(), event.amount(), event.currency());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        RefundAuditRecord record = RefundAuditRecord.create(
                event.eventId(),
                event.refundId(),
                event.paymentId(),
                event.walletId(),
                event.userId(),
                event.amount(),
                event.currency(),
                event.reason(),
                event.reference(),
                event.correlationId(),
                event.timestamp(),
                Instant.now()
        );

        refundAuditRecordService.save(record);

        log.info("RefundAuditRecord persisted: id={}, refundId={}", record.id(), record.refundId());
    }
}
