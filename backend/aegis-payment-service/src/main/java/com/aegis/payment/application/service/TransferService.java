package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.port.inbound.GetTransferUseCase;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing the transfer use cases.
 *
 * <p>The {@link #execute(TransferFundsUseCase.TransferCommand)} method validates the
 * command and persists the transfer in {@code PENDING} state. The saga orchestration
 * (fraud check, hold, settlement) is deliberately not implemented in this scaffold
 * and lands in issues #249–#251.</p>
 */
@Service
public class TransferService implements TransferFundsUseCase, GetTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;

    public TransferService(TransferRepository transferRepository,
                           EventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Transfer execute(TransferCommand command) {
        validate(command);

        if (transferRepository.existsBySourceWalletIdAndReference(
                command.sourceWalletId(), command.reference())) {
            throw new DuplicateTransferException(command.reference());
        }

        Transfer transfer = Transfer.request(
                command.sourceWalletId(),
                command.destWalletId(),
                command.userId(),
                command.amount(),
                command.currency(),
                command.description(),
                command.reference()
        );

        Transfer saved = transferRepository.save(transfer);
        eventPublisher.publish(new TransferRequested(saved));
        log.info("Transfer requested: id={}, reference={}", saved.getId(), saved.getReference());

        // Saga orchestration (fraud check → hold → settle) lands in #249-#251.
        throw new UnsupportedOperationException(
                "Saga orchestration lands in #249-#251");
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer findById(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    private void validate(TransferCommand command) {
        if (command.sourceWalletId() == null) {
            throw new IllegalArgumentException("sourceWalletId is required");
        }
        if (command.destWalletId() == null) {
            throw new IllegalArgumentException("destWalletId is required");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (command.currency() == null || !command.currency().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter uppercase ISO code");
        }
        if (command.reference() == null || command.reference().isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (command.sourceWalletId().equals(command.destWalletId())) {
            throw new SelfTransferException(command.sourceWalletId());
        }
    }
}
