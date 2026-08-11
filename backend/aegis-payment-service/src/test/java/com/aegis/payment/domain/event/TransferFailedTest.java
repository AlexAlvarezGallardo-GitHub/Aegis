package com.aegis.payment.domain.event;

import com.aegis.payment.domain.model.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the TransferFailed domain event.
 */
@DisplayName("TransferFailed - Domain Event")
class TransferFailedTest {

    @Nested
    @DisplayName("When constructing from a failed Transfer")
    class WhenConstructingFromTransfer {

        @Test
        @DisplayName("Should populate envelope and payload fields from the aggregate")
        void shouldPopulateFieldsFromTransfer() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Transfer transfer = Transfer.request(source, dest, user,
                    new BigDecimal("75.50"), "USD", "refund", "ref-200");
            transfer.fail("insufficient funds");

            TransferFailed event = new TransferFailed(transfer);

            assertNotNull(event.eventId());
            assertEquals(7, event.eventId().version());
            assertEquals("TRANSFER_FAILED", event.eventType());
            assertEquals("1.0", event.schemaVersion());
            assertNotNull(event.occurredAt());
            assertEquals(transfer.getId(), event.causationId());
            assertEquals(transfer.getId(), event.correlationId());
            assertEquals(transfer.getId(), event.aggregateId());
            assertEquals("TRANSFER", event.aggregateType());
            assertEquals(transfer.getId(), event.transferId());
            assertEquals(source, event.sourceWalletId());
            assertEquals(dest, event.destWalletId());
            assertEquals(user, event.userId());
            assertEquals(new BigDecimal("75.50"), event.amount());
            assertEquals("USD", event.currency());
            assertEquals("insufficient funds", event.failureReason());
        }
    }

    @Nested
    @DisplayName("When constructing with canonical constructor")
    class WhenConstructingWithCanonical {

        @Test
        @DisplayName("Should retain all provided values")
        void shouldRetainAllValues() {
            UUID eventId = UUID.randomUUID();
            UUID transferId = UUID.randomUUID();
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            java.time.Instant now = java.time.Instant.now();

            TransferFailed event = new TransferFailed(
                    eventId, "TRANSFER_FAILED", "1.0", now,
                    transferId, transferId, transferId, "TRANSFER",
                    transferId, source, dest, user,
                    new BigDecimal("10.00"), "USD", "timeout");

            assertEquals(eventId, event.eventId());
            assertEquals("TRANSFER_FAILED", event.eventType());
            assertEquals("1.0", event.schemaVersion());
            assertEquals(now, event.occurredAt());
            assertEquals("timeout", event.failureReason());
        }
    }
}
