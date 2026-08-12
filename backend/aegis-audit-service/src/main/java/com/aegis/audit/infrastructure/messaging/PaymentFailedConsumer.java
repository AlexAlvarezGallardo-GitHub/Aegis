package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.PaymentAuditRecordService;
import com.aegis.audit.domain.event.PaymentFailedEvent;
import com.aegis.audit.domain.model.PaymentAuditRecord;
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
 * Kafka consumer that listens for {@code payment.failed} events.
 * <p>
 * Each incoming event is mapped to a {@link PaymentAuditRecord} and persisted
 * to provide a durable audit trail. Event identifiers are recorded in the
 * {@code processed_events} table so that at-least-once delivery does not
 * produce duplicate audit records.</p>
 */
@Component
public class PaymentFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);

    private final PaymentAuditRecordService paymentAuditRecordService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public PaymentFailedConsumer(PaymentAuditRecordService paymentAuditRecordService,
                                  ProcessedEventJpaRepository processedEventRepository) {
        this.paymentAuditRecordService = paymentAuditRecordService;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Consumes a {@link PaymentFailedEvent} from Kafka, maps it to a
     * {@link PaymentAuditRecord}, and persists it to the database.
     *
     * @param event     the deserialized payment failed event
     * @param topic     the source topic
     * @param partition the source partition
     * @param offset    the source offset
     */
    @KafkaListener(
            topics = "${aegis.kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentFailedListenerContainerFactory"
    )
    @Transactional
    public void consume(PaymentFailedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received PaymentFailedEvent: eventId={}, paymentId={}, reason={}",
                event.eventId(), event.paymentId(), event.failureReason());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        PaymentAuditRecord record = PaymentAuditRecord.create(
                event.eventId(),
                event.paymentId(),
                "FAILED",
                event.walletId(),
                event.userId(),
                event.amount(),
                event.currency(),
                null,
                event.failureReason(),
                event.correlationId(),
                event.timestamp(),
                Instant.now()
        );

        paymentAuditRecordService.save(record);

        log.info("PaymentAuditRecord persisted: id={}, paymentId={}", record.id(), record.paymentId());
    }
}
