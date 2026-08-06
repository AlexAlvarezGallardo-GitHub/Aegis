package com.aegis.reporting.infrastructure.messaging;

import com.aegis.reporting.application.service.BalanceProjectionService;
import com.aegis.reporting.domain.event.FundsDepositedEvent;
import com.aegis.reporting.domain.model.BalanceProjection;
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
 * Kafka consumer that listens for {@code FundsDepositedEvent} messages on the
 * {@code wallet.funds.deposited} topic and keeps the balance projection up to
 * date. Event identifiers are recorded in {@code processed_events} so that
 * at-least-once delivery does not apply a deposit twice.
 */
@Component
public class FundsDepositedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FundsDepositedConsumer.class);

    private final BalanceProjectionService balanceProjectionService;
    private final ProcessedEventJpaRepository processedEventRepository;

    public FundsDepositedConsumer(BalanceProjectionService balanceProjectionService,
                                  ProcessedEventJpaRepository processedEventRepository) {
        this.balanceProjectionService = balanceProjectionService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(
            topics = "${aegis.kafka.topics.funds-deposited}",
            groupId = "${spring.kafka.consumer.group-id}",
            clientIdPrefix = "reporting"
    )
    @Transactional
    public void consume(FundsDepositedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received FundsDepositedEvent eventId={} walletId={} amount={} {}",
                event.eventId(), event.walletId(), event.amount(), event.currency());

        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(), topic, partition, offset, Instant.now());
        if (inserted == 0) {
            log.debug("Skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        BalanceProjection projection = balanceProjectionService.findByWalletId(event.walletId())
                .map(existing -> existing.withUpdatedBalance(event.newBalance(), event.timestamp()))
                .orElseGet(() -> BalanceProjection.create(
                        event.walletId(),
                        event.userId(),
                        event.newBalance(),
                        event.currency(),
                        event.timestamp()
                ));

        balanceProjectionService.save(projection);

        log.info("Balance projection saved walletId={} balance={} {}",
                event.walletId(), projection.balance(), projection.currency());
    }
}
