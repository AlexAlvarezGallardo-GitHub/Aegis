package com.aegis.reporting.infrastructure.messaging;

import com.aegis.reporting.application.service.TransferProjectionService;
import com.aegis.reporting.domain.event.TransferFailedEvent;
import com.aegis.reporting.domain.model.TransferProjection;
import com.aegis.reporting.infrastructure.persistence.ProcessedEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Kafka consumer that listens for {@code TransferFailedEvent} messages on the
 * {@code payment.transfer.failed} topic and keeps the transfer projection up to
 * date. Event identifiers are recorded in {@code processed_events} so that
 * at-least-once delivery does not apply a failure twice.
 */
@Component
public class TransferFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferFailedConsumer.class);

    private final TransferProjectionService transferProjectionService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public TransferFailedConsumer(TransferProjectionService transferProjectionService,
                                   ProcessedEventJpaRepository processedEventRepository) {
        this.transferProjectionService = transferProjectionService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(
            topics = "${aegis.kafka.topics.transfer-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            clientIdPrefix = "reporting"
    )
    @Transactional
    public void consume(TransferFailedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received TransferFailedEvent eventId={} transferId={} reason={}",
                event.eventId(), event.transferId(), event.failureReason());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        TransferProjection projection = transferProjectionService.findByTransferId(event.transferId())
                .map(existing -> existing.withStatus("FAILED", event.failureReason(), event.timestamp()))
                .orElseGet(() -> TransferProjection.create(
                        event.transferId(),
                        event.sourceWalletId(),
                        event.destWalletId(),
                        event.userId(),
                        event.amount(),
                        event.currency(),
                        "FAILED",
                        event.failureReason(),
                        event.timestamp()
                ));

        transferProjectionService.save(projection);

        log.info("Transfer projection saved transferId={} status={}", event.transferId(), projection.status());
    }
}
