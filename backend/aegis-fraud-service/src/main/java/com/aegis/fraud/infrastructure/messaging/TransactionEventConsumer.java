package com.aegis.fraud.infrastructure.messaging;

import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final AssessFraudUseCase assessFraudUseCase;

    public TransactionEventConsumer(AssessFraudUseCase assessFraudUseCase) {
        this.assessFraudUseCase = assessFraudUseCase;
    }

    @KafkaListener(topics = "${aegis.kafka.topics.payment-transfer-requested}", groupId = "fraud-group")
    public void onTransferRequested(TransferRequestedEvent event) {
        log.info("Received TransferRequestedEvent for transaction {}", event.transactionId());
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
