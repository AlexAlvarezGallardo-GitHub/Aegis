package com.aegis.fraud.infrastructure.messaging;

import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.infrastructure.persistence.ProcessedEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final AssessFraudUseCase assessFraudUseCase;
    private final ProcessedEventJpaRepository processedEventRepository;

    public TransactionEventConsumer(AssessFraudUseCase assessFraudUseCase,
                                    ProcessedEventJpaRepository processedEventRepository) {
        this.assessFraudUseCase = assessFraudUseCase;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "${aegis.kafka.topics.payment-transfer-requested}", groupId = "fraud-group")
    @Transactional
    public void onTransferRequested(TransferRequestedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received TransferRequestedEvent for transaction {}", event.transactionId());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        assessFraudUseCase.assess(new AssessFraudUseCase.AssessmentCommand(
                event.transactionId(),
                "TRANSFER",
                event.amount(),
                event.currency(),
                event.sourceWalletId(),
                event.destWalletId(),
                event.userId(),
                null));
    }

    public record TransferRequestedEvent(
            UUID eventId,
            UUID transactionId,
            BigDecimal amount,
            String currency,
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId
    ) {}
}
