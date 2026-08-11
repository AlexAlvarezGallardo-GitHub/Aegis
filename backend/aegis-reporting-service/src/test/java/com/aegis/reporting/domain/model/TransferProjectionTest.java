package com.aegis.reporting.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferProjection - Domain Model")
class TransferProjectionTest {

    @Test
    @DisplayName("Should create TransferProjection with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        Instant now = Instant.now();

        TransferProjection projection = TransferProjection.create(
                transferId, sourceWalletId, destWalletId, userId,
                amount, "USD", "COMPLETED", null, now
        );

        assertNotNull(projection.id());
        assertEquals(transferId, projection.transferId());
        assertEquals(sourceWalletId, projection.sourceWalletId());
        assertEquals(destWalletId, projection.destWalletId());
        assertEquals(userId, projection.userId());
        assertEquals(amount, projection.amount());
        assertEquals("USD", projection.currency());
        assertEquals("COMPLETED", projection.status());
        assertNull(projection.failureReason());
        assertEquals(now, projection.eventTimestamp());
    }

    @Test
    @DisplayName("Should throw NullPointerException when transferId is null")
    void shouldThrowWhenTransferIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when sourceWalletId is null")
    void shouldThrowWhenSourceWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when destWalletId is null")
    void shouldThrowWhenDestWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                        BigDecimal.TEN, "USD", "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when amount is null")
    void shouldThrowWhenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        null, "USD", "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, null, "COMPLETED", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when status is null")
    void shouldThrowWhenStatusIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferProjection.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "COMPLETED", null, null));
    }

    @Test
    @DisplayName("Should return new instance with updated status")
    void shouldReturnNewInstanceWithUpdatedStatus() {
        UUID id = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant oldTime = Instant.now().minusSeconds(60);
        Instant newTime = Instant.now();

        TransferProjection original = new TransferProjection(
                id, transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "COMPLETED", null, oldTime
        );

        TransferProjection updated = original.withStatus("FAILED", "INSUFFICIENT_FUNDS", newTime);

        // Original unchanged
        assertEquals("COMPLETED", original.status());
        assertNull(original.failureReason());
        assertEquals(oldTime, original.eventTimestamp());

        // Updated has new values but same id and transferId
        assertEquals(id, updated.id());
        assertEquals(transferId, updated.transferId());
        assertEquals(sourceWalletId, updated.sourceWalletId());
        assertEquals(destWalletId, updated.destWalletId());
        assertEquals(userId, updated.userId());
        assertEquals(new BigDecimal("100.00"), updated.amount());
        assertEquals("USD", updated.currency());
        assertEquals("FAILED", updated.status());
        assertEquals("INSUFFICIENT_FUNDS", updated.failureReason());
        assertEquals(newTime, updated.eventTimestamp());
    }
}
