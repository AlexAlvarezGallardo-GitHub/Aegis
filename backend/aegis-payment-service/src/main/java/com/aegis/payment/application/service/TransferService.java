package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.TransferFailed;
import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.exception.FraudRejectedException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.port.inbound.GetTransferUseCase;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.FraudDecision;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.TransactionContext;
import com.aegis.payment.domain.port.outbound.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application service implementing the transfer use cases.
 *
 * <p>The {@link #execute(TransferFundsUseCase.TransferCommand)} method implements the
 * fraud-check step of the transfer saga in two transactional phases:</p>
 * <ol>
 *   <li>Phase 1 — persist PENDING transfer + publish TransferRequested (committed)</li>
 *   <li>Phase 2 — synchronous fraud assessment; on REJECT or unavailability, persist
 *       FAILED + publish TransferFailed, then throw the appropriate domain exception</li>
 * </ol>
 *
 * <p>Hold and settlement steps land in follow-up issues (#249).</p>
 */
@Service
public class TransferService implements TransferFundsUseCase, GetTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;
    private final FraudAssessmentGateway fraudAssessmentGateway;
    private final TransactionTemplate transactionTemplate;

    public TransferService(TransferRepository transferRepository,
                           EventPublisher eventPublisher,
                           FraudAssessmentGateway fraudAssessmentGateway,
                           TransactionTemplate transactionTemplate) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
        this.fraudAssessmentGateway = fraudAssessmentGateway;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Transfer execute(TransferCommand command) {
        // Phase 1 — persist PENDING transfer + publish TransferRequested (own transaction)
        Transfer transfer = transactionTemplate.execute(status ->
                persistPendingTransfer(command));

        // Phase 2 — fraud assessment (own transaction; failure state is committed before throwing)
        return executeFraudCheck(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer findById(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    private Transfer persistPendingTransfer(TransferCommand command) {
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
        return saved;
    }

    private Transfer executeFraudCheck(Transfer transfer) {
        AtomicReference<RuntimeException> postCommitException = new AtomicReference<>();

        Transfer result = transactionTemplate.execute(status -> {
            transfer.startFraudCheck();
            transferRepository.save(transfer);

            FraudDecision decision;
            try {
                TransactionContext ctx = new TransactionContext(
                        transfer.getId(),
                        transfer.getSourceWalletId(),
                        transfer.getDestWalletId(),
                        transfer.getUserId(),
                        transfer.getAmount(),
                        transfer.getCurrency()
                );
                decision = fraudAssessmentGateway.assess(ctx);
            } catch (FraudAssessmentUnavailableException ex) {
                transfer.fail("FRAUD_UNAVAILABLE");
                transferRepository.save(transfer);
                eventPublisher.publish(new TransferFailed(transfer));
                log.warn("Fraud assessment unavailable for transfer {}: {}",
                        transfer.getId(), ex.getMessage());
                postCommitException.set(ex);
                return transfer;
            }

            if (decision == FraudDecision.REJECT) {
                transfer.fail("FRAUD_REJECTED");
                transferRepository.save(transfer);
                eventPublisher.publish(new TransferFailed(transfer));
                log.info("Transfer rejected by fraud assessment: id={}", transfer.getId());
                postCommitException.set(new FraudRejectedException(transfer.getId()));
                return transfer;
            }

            // APPROVE or REVIEW — leave in FRAUD_CHECK for hold/settle in the next saga step
            log.info("Fraud assessment passed for transfer {}: decision={}",
                    transfer.getId(), decision);
            return transfer;
        });

        if (postCommitException.get() != null) {
            throw postCommitException.get();
        }
        return result;
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
