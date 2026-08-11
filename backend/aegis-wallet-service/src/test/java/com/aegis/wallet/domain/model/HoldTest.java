package com.aegis.wallet.domain.model;

import com.aegis.wallet.domain.exception.HoldNotActiveException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HoldTest {

    @Nested
    @DisplayName("Hold.reserve factory")
    class Reserve {

        @Test
        void shouldCreateActiveHoldWithValidInputs() {
            UUID walletId = UUID.randomUUID();
            Hold hold = Hold.reserve(walletId, new BigDecimal("100.00"), "EUR", "TXN-1");

            assertNotNull(hold.getId());
            assertEquals(walletId, hold.getWalletId());
            assertEquals(0, new BigDecimal("100.00").compareTo(hold.getAmount()));
            assertEquals("EUR", hold.getCurrency());
            assertEquals("TXN-1", hold.getReference());
            assertEquals(HoldStatus.ACTIVE, hold.getStatus());
            assertNotNull(hold.getCreatedAt());
            assertNotNull(hold.getExpiresAt());
            assertTrue(hold.getExpiresAt().isAfter(hold.getCreatedAt()));
        }

        @Test
        void shouldRejectNullWalletId() {
            assertThrows(NullPointerException.class,
                    () -> Hold.reserve(null, new BigDecimal("10.00"), "EUR", "ref"));
        }

        @Test
        void shouldRejectNullAmount() {
            assertThrows(NullPointerException.class,
                    () -> Hold.reserve(UUID.randomUUID(), null, "EUR", "ref"));
        }

        @Test
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Hold.reserve(UUID.randomUUID(), BigDecimal.ZERO, "EUR", "ref"));
        }

        @Test
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Hold.reserve(UUID.randomUUID(), new BigDecimal("-1.00"), "EUR", "ref"));
        }

        @Test
        void shouldRejectNullCurrency() {
            assertThrows(NullPointerException.class,
                    () -> Hold.reserve(UUID.randomUUID(), new BigDecimal("10.00"), null, "ref"));
        }

        @Test
        void shouldRejectNullReference() {
            assertThrows(NullPointerException.class,
                    () -> Hold.reserve(UUID.randomUUID(), new BigDecimal("10.00"), "EUR", null));
        }

        @Test
        void shouldRejectBlankReference() {
            assertThrows(IllegalArgumentException.class,
                    () -> Hold.reserve(UUID.randomUUID(), new BigDecimal("10.00"), "EUR", "   "));
        }

        @Test
        void shouldNormalizeCurrencyToUpperCase() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("10.00"), "eur", "ref");
            assertEquals("EUR", hold.getCurrency());
        }

        @Test
        void shouldScaleAmountToTwoDecimals() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("10.555"), "EUR", "ref");
            assertEquals(2, hold.getAmount().scale());
        }
    }

    @Nested
    @DisplayName("settle()")
    class Settle {

        @Test
        void shouldTransitionActiveToSettled() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.settle();
            assertEquals(HoldStatus.SETTLED, hold.getStatus());
        }

        @Test
        void shouldThrowWhenSettlingNonActiveHold() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.release();
            assertThrows(HoldNotActiveException.class, hold::settle);
        }

        @Test
        void shouldThrowWhenSettlingAlreadySettledHold() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.settle();
            assertThrows(HoldNotActiveException.class, hold::settle);
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        void shouldTransitionActiveToReleased() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            boolean changed = hold.release();
            assertTrue(changed);
            assertEquals(HoldStatus.RELEASED, hold.getStatus());
        }

        @Test
        void shouldBeIdempotentWhenAlreadyReleased() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.release();
            boolean changed = hold.release();
            assertFalse(changed);
            assertEquals(HoldStatus.RELEASED, hold.getStatus());
        }

        @Test
        void shouldThrowWhenReleasingSettledHold() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.settle();
            assertThrows(HoldNotActiveException.class, hold::release);
        }

        @Test
        void shouldThrowWhenReleasingExpiredHold() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.expire();
            assertThrows(HoldNotActiveException.class, hold::release);
        }
    }

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        void shouldTransitionActiveToExpired() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.expire();
            assertEquals(HoldStatus.EXPIRED, hold.getStatus());
        }

        @Test
        void shouldThrowWhenExpiringNonActiveHold() {
            Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("50.00"), "EUR", "ref");
            hold.settle();
            assertThrows(HoldNotActiveException.class, hold::expire);
        }
    }
}
