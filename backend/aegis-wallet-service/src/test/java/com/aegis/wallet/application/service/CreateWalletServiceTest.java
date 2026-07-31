package com.aegis.wallet.application.service;

import com.aegis.wallet.application.dto.CreateWalletCommand;
import com.aegis.wallet.domain.event.FundsDeposited;
import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.event.WalletCreated;
import com.aegis.wallet.domain.exception.WalletLimitExceededException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateWalletServiceTest {

    private StubWalletRepository walletRepository;
    private StubEventPublisher eventPublisher;
    private CreateWalletService service;

    @BeforeEach
    void setUp() {
        walletRepository = new StubWalletRepository();
        eventPublisher = new StubEventPublisher();
        service = new CreateWalletService(walletRepository, eventPublisher, 5);
    }

    @Test
    void createWalletShouldSucceed() {
        UUID userId = UUID.randomUUID();
        CreateWalletCommand command = new CreateWalletCommand("EUR", "corr-1");
        var response = service.createAndReturnResponse(command, userId);

        assertNotNull(response.walletId());
        assertEquals(userId, response.userId());
        assertEquals(java.math.BigDecimal.ZERO, response.balance());
        assertEquals("EUR", response.currency());
        assertEquals("ACTIVE", response.status());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void createWalletShouldFailWhenLimitExceeded() {
        UUID userId = UUID.randomUUID();
        walletRepository = new StubWalletRepository(5);
        service = new CreateWalletService(walletRepository, eventPublisher, 5);

        CreateWalletCommand command = new CreateWalletCommand("EUR", "corr-1");

        assertThrows(WalletLimitExceededException.class,
                () -> service.createAndReturnResponse(command, userId));
    }

    @Test
    void createWalletShouldUseProvidedCorrelationId() {
        UUID userId = UUID.randomUUID();
        CreateWalletCommand command = new CreateWalletCommand("EUR", "my-corr-id");
        service.createAndReturnResponse(command, userId);

        WalletCreated event = eventPublisher.events.getFirst();
        assertEquals("my-corr-id", event.correlationId());
    }

    @Test
    void createWalletShouldGenerateCorrelationIdWhenMissing() {
        UUID userId = UUID.randomUUID();
        CreateWalletCommand command = new CreateWalletCommand("EUR", null);
        service.createAndReturnResponse(command, userId);

        WalletCreated event = eventPublisher.events.getFirst();
        assertNotNull(event.correlationId());
    }

    private static class StubWalletRepository implements WalletRepository {
        private final long existingCount;
        private final List<Wallet> wallets = new ArrayList<>();

        StubWalletRepository() { this.existingCount = 0; }
        StubWalletRepository(long existingCount) { this.existingCount = existingCount; }

        @Override
        public Wallet save(Wallet wallet) {
            wallets.add(wallet);
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return wallets.stream()
                    .filter(w -> w.getWalletId().equals(walletId))
                    .findFirst();
        }

        @Override
        public List<Wallet> findByUserId(UUID userId) {
            return wallets.stream()
                    .filter(w -> w.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public long countByUserId(UUID userId) {
            return existingCount;
        }
    }

    private static class StubEventPublisher implements EventPublisher {
        final List<WalletCreated> events = new ArrayList<>();

        @Override
        public void publish(WalletCreated event) {
            events.add(event);
        }

        @Override
        public void publish(WalletBalanceAdjusted event) {
        }

        @Override
        public void publish(FundsDeposited event) {
        }
    }
}
