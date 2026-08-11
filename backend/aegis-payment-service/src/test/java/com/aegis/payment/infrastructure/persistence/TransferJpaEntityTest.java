package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferJpaEntity - Persistence Entity")
class TransferJpaEntityTest {

    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID DEST = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final String REFERENCE = "ref-001";

    private Transfer completedTransfer() {
        Transfer transfer = Transfer.request(
                SOURCE, DEST, USER, new BigDecimal("100.00"), "EUR", "desc", REFERENCE);
        transfer.startFraudCheck();
        transfer.markFraudAssessed(UUID.randomUUID());
        transfer.markFundsReserved(UUID.randomUUID());
        transfer.complete();
        return transfer;
    }

    @Nested
    @DisplayName("When mapping from domain Transfer")
    class WhenMappingFromDomain {

        @Test
        @DisplayName("Should initialize all fields from the aggregate")
        void shouldInitializeAllFields() {
            Transfer transfer = completedTransfer();

            TransferJpaEntity entity = new TransferJpaEntity(transfer);

            assertEquals(transfer.getId(), entity.getId());
            assertEquals(SOURCE, entity.getSourceWalletId());
            assertEquals(DEST, entity.getDestWalletId());
            assertEquals(USER, entity.getUserId());
            assertEquals(new BigDecimal("100.00"), entity.getAmount());
            assertEquals("EUR", entity.getCurrency());
            assertEquals("desc", entity.getDescription());
            assertEquals(REFERENCE, entity.getReference());
            assertEquals(TransferStatus.COMPLETED, entity.getStatus());
            assertNotNull(entity.getFraudAssessmentId());
            assertNotNull(entity.getHoldId());
            assertNull(entity.getFailureReason());
            assertEquals(transfer.getCreatedAt(), entity.getCreatedAt());
            assertEquals(transfer.getUpdatedAt(), entity.getUpdatedAt());
            assertNotNull(entity.getCompletedAt());
        }

        @Test
        @DisplayName("Should map nullable fields correctly for a pending transfer")
        void shouldMapNullableFields() {
            Transfer transfer = Transfer.request(
                    SOURCE, DEST, USER, new BigDecimal("50.00"), "EUR", null, REFERENCE);

            TransferJpaEntity entity = new TransferJpaEntity(transfer);

            assertEquals(TransferStatus.PENDING, entity.getStatus());
            assertNull(entity.getFraudAssessmentId());
            assertNull(entity.getHoldId());
            assertNull(entity.getFailureReason());
            assertNull(entity.getCompletedAt());
            assertNull(entity.getDescription());
        }
    }

    @Nested
    @DisplayName("When converting back to domain Transfer")
    class WhenConvertingToDomain {

        @Test
        @DisplayName("Should rehydrate a full completed transfer")
        void shouldRehydrateCompletedTransfer() {
            Transfer original = completedTransfer();
            TransferJpaEntity entity = new TransferJpaEntity(original);

            Transfer rehydrated = entity.toDomain();

            assertEquals(original.getId(), rehydrated.getId());
            assertEquals(SOURCE, rehydrated.getSourceWalletId());
            assertEquals(DEST, rehydrated.getDestWalletId());
            assertEquals(USER, rehydrated.getUserId());
            assertEquals(original.getAmount(), rehydrated.getAmount());
            assertEquals("EUR", rehydrated.getCurrency());
            assertEquals("desc", rehydrated.getDescription());
            assertEquals(REFERENCE, rehydrated.getReference());
            assertEquals(TransferStatus.COMPLETED, rehydrated.getStatus());
            assertEquals(original.getFraudAssessmentId(), rehydrated.getFraudAssessmentId());
            assertEquals(original.getHoldId(), rehydrated.getHoldId());
            assertEquals(original.getCompletedAt(), rehydrated.getCompletedAt());
            assertEquals(original.getCreatedAt(), rehydrated.getCreatedAt());
        }
    }
}
