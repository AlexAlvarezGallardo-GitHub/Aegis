package com.aegis.wallet.application.service;

import com.aegis.wallet.application.dto.CreateWalletCommand;
import com.aegis.wallet.application.dto.WalletResponse;
import com.aegis.wallet.domain.event.WalletCreated;
import com.aegis.wallet.domain.exception.WalletLimitExceededException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.port.inbound.CreateWalletUseCase;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateWalletService implements CreateWalletUseCase {

    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;
    private final int maxWalletsPerUser;

    public CreateWalletService(WalletRepository walletRepository,
                                EventPublisher eventPublisher,
                                @Value("${aegis.wallet.max-per-user:5}") int maxWalletsPerUser) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.maxWalletsPerUser = maxWalletsPerUser;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        long walletCount = walletRepository.countByUserId(command.userId());
        if (walletCount >= maxWalletsPerUser) {
            throw new WalletLimitExceededException(maxWalletsPerUser);
        }

        Wallet wallet = Wallet.create(command.userId(), command.currency());
        Wallet savedWallet = walletRepository.save(wallet);

        WalletCreated event = savedWallet.toCreatedEvent(command.correlationId());
        eventPublisher.publish(event);

        return new Result(
                savedWallet.getWalletId().value(),
                savedWallet.getUserId(),
                savedWallet.getBalance(),
                savedWallet.getCurrency(),
                savedWallet.getStatus().name(),
                savedWallet.getCreatedAt()
        );
    }

    /**
     * Convenience method for the controller to create a wallet and return a response DTO.
     *
     * @param command the create wallet command from the web layer
     * @param userId  the authenticated user id from the request header
     * @return the wallet response DTO
     */
    public WalletResponse createAndReturnResponse(CreateWalletCommand command, UUID userId) {
        Command useCaseCommand = new Command(
                userId,
                command.currency(),
                command.correlationId() != null ? command.correlationId() : UUID.randomUUID().toString()
        );

        Result result = execute(useCaseCommand);

        return new WalletResponse(
                result.walletId(),
                result.userId(),
                result.balance(),
                result.currency(),
                result.status(),
                false,
                result.createdAt()
        );
    }
}
