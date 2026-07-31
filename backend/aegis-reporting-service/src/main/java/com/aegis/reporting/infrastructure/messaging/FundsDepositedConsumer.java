package com.aegis.reporting.infrastructure.messaging;

import com.aegis.reporting.domain.event.FundsDepositedEvent;
import com.aegis.reporting.domain.model.BalanceProjection;
import com.aegis.reporting.infrastructure.persistence.BalanceProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer that listens for {@code FundsDepositedEvent} messages on the
 * {@code wallet.funds.deposited} topic and keeps the balance projection up to date.
 */
@Component
public class FundsDepositedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FundsDepositedConsumer.class);

    private final BalanceProjectionRepository repository;

    public FundsDepositedConsumer(BalanceProjectionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "wallet.funds.deposited",
            groupId = "${spring.kafka.consumer.group-id}",
            clientIdPrefix = "reporting"
    )
    public void consume(FundsDepositedEvent event) {
        log.info("Received FundsDepositedEvent eventId={} walletId={} amount={} {}",
                event.eventId(), event.walletId(), event.amount(), event.currency());

        BalanceProjection projection = repository.findByWalletId(event.walletId())
                .map(existing -> {
                    existing.updateBalance(event.newBalance(), event.timestamp());
                    return existing;
                })
                .orElseGet(() -> new BalanceProjection(
                        UUID.randomUUID(),
                        event.walletId(),
                        event.userId(),
                        event.newBalance(),
                        event.currency(),
                        event.timestamp()
                ));

        repository.save(projection);

        log.info("Balance projection saved walletId={} balance={} {}",
                event.walletId(), projection.getBalance(), projection.getCurrency());
    }
}
