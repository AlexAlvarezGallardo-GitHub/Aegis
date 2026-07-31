package com.aegis.reporting.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BalanceProjection - Domain Model")
class BalanceProjectionTest {

    @Test
    @DisplayName("Should create BalanceProjection with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("1000.00");
        Instant now = Instant.now();

        BalanceProjection projection = BalanceProjection.create(walletId, userId, balance, "USD", now);

        assertNotNull(projection.id());
        assertEquals(walletId, projection.walletId());
        assertEquals(userId, projection.userId());
        assertEquals(balance, projection.balance());
        assertEquals("USD", projection.currency());
        assertEquals(now, projection.lastUpdated());
    }

    @Test
    @DisplayName("Should throw NullPointerException when walletId is null")
    void shouldThrowWhenWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                BalanceProjection.create(null, UUID.randomUUID(), BigDecimal.TEN, "USD", Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                BalanceProjection.create(UUID.randomUUID(), null, BigDecimal.TEN, "USD", Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when balance is null")
    void shouldThrowWhenBalanceIsNull() {
        assertThrows(NullPointerException.class, () ->
                BalanceProjection.create(UUID.randomUUID(), UUID.randomUUID(), null, "USD", Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                BalanceProjection.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when lastUpdated is null")
    void shouldThrowWhenLastUpdatedIsNull() {
        assertThrows(NullPointerException.class, () ->
                BalanceProjection.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD", null));
    }

    @Test
    @DisplayName("Should return new instance with updated balance")
    void shouldReturnNewInstanceWithUpdatedBalance() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant oldTime = Instant.now().minusSeconds(60);
        Instant newTime = Instant.now();

        BalanceProjection original = new BalanceProjection(id, walletId, userId,
                new BigDecimal("500.00"), "USD", oldTime);

        BalanceProjection updated = original.withUpdatedBalance(new BigDecimal("1000.00"), newTime);

        // Original unchanged
        assertEquals(new BigDecimal("500.00"), original.balance());
        assertEquals(oldTime, original.lastUpdated());

        // Updated has new values but same id and walletId
        assertEquals(id, updated.id());
        assertEquals(walletId, updated.walletId());
        assertEquals(userId, updated.userId());
        assertEquals(new BigDecimal("1000.00"), updated.balance());
        assertEquals("USD", updated.currency());
        assertEquals(newTime, updated.lastUpdated());
    }
}
