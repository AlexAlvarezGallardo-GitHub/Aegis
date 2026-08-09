package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.DuplicateDepositException;
import com.aegis.wallet.domain.port.inbound.CreateWalletUseCase;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase.DepositCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that concurrent deposits using the same idempotency reference are
 * rejected at the database level (unique partial index V3), so exactly one
 * deposit succeeds regardless of the in-memory check racing.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Deposit idempotency - concurrent duplicate references")
class ConcurrentDepositIdempotencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_wallet_idempotency")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private DepositFundsUseCase depositFundsUseCase;

    @Autowired
    private CreateWalletUseCase createWalletUseCase;

    @BeforeEach
    void setUp() {
        // nothing to clean: each test uses a fresh wallet
    }

    @Test
    @DisplayName("Only one of several concurrent deposits with the same reference should succeed")
    void onlyOneConcurrentDepositWithSameReferenceShouldSucceed() throws Exception {
        // Arrange - create a wallet first
        CreateWalletUseCase.Result wallet = createWalletUseCase.execute(
                new CreateWalletUseCase.Command(UUID.randomUUID(), "EUR", "corr-create"));
        UUID walletId = wallet.walletId();
        UUID userId = wallet.userId();
        String reference = "TXN-CONCURRENT-" + UUID.randomUUID();

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger duplicateCount = new AtomicInteger();

        // Act - fire N concurrent deposits with the same reference
        Future<?>[] futures = new Future<?>[threads];
        for (int i = 0; i < threads; i++) {
            futures[i] = executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    depositFundsUseCase.deposit(new DepositCommand(
                            walletId, userId, new BigDecimal("10.00"), "EUR",
                            "BANK_TRANSFER", reference, "corr-" + UUID.randomUUID()));
                    successCount.incrementAndGet();
                } catch (DuplicateDepositException e) {
                    duplicateCount.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    // Under true concurrency the DB unique index (V3) is what
                    // rejects the losing deposits, so treat it as a duplicate too.
                    duplicateCount.incrementAndGet();
                }
                return null;
            });
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Assert - exactly one deposit wins, all others are rejected as duplicates
        assertEquals(1, successCount.get(),
                "Exactly one deposit should succeed, but got: " + successCount.get());
        assertEquals(threads - 1, duplicateCount.get(),
                "All remaining deposits should be rejected as duplicates, but got: " + duplicateCount.get());
    }
}

