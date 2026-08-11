package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.ErrorStatus;
import com.aegis.payment.domain.model.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment domain exceptions")
class DomainExceptionTest {

    @Test
    @DisplayName("TransferNotFoundException should carry NOT_FOUND status")
    void transferNotFound() {
        UUID id = UUID.randomUUID();
        TransferNotFoundException ex = new TransferNotFoundException(id);
        assertEquals("TRANSFER_NOT_FOUND", ex.getCode());
        assertEquals(ErrorStatus.NOT_FOUND, ex.getErrorStatus());
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("DuplicateTransferException should carry CONFLICT status")
    void duplicateTransfer() {
        DuplicateTransferException ex = new DuplicateTransferException("ref-001");
        assertEquals("TRANSFER_DUPLICATE", ex.getCode());
        assertEquals(ErrorStatus.CONFLICT, ex.getErrorStatus());
        assertTrue(ex.getMessage().contains("ref-001"));
    }

    @Test
    @DisplayName("InvalidTransferStateException should carry BAD_REQUEST status")
    void invalidState() {
        InvalidTransferStateException ex =
                new InvalidTransferStateException(TransferStatus.PENDING, TransferStatus.COMPLETED);
        assertEquals("INVALID_TRANSFER_STATE", ex.getCode());
        assertEquals(ErrorStatus.BAD_REQUEST, ex.getErrorStatus());
        assertTrue(ex.getMessage().contains("PENDING"));
        assertTrue(ex.getMessage().contains("COMPLETED"));
    }

    @Test
    @DisplayName("SelfTransferException should carry BAD_REQUEST status")
    void selfTransfer() {
        UUID walletId = UUID.randomUUID();
        SelfTransferException ex = new SelfTransferException(walletId);
        assertEquals("SELF_TRANSFER", ex.getCode());
        assertEquals(ErrorStatus.BAD_REQUEST, ex.getErrorStatus());
        assertTrue(ex.getMessage().contains(walletId.toString()));
    }

    @Test
    @DisplayName("FraudRejectedException should carry UNPROCESSABLE_ENTITY status")
    void fraudRejected() {
        UUID transferId = UUID.randomUUID();
        FraudRejectedException ex = new FraudRejectedException(transferId);
        assertEquals("TRANSFER_REJECTED_BY_FRAUD", ex.getCode());
        assertEquals(ErrorStatus.UNPROCESSABLE_ENTITY, ex.getErrorStatus());
        assertTrue(ex.getMessage().contains(transferId.toString()));
    }

    @Test
    @DisplayName("FraudAssessmentUnavailableException should carry SERVICE_UNAVAILABLE status")
    void fraudUnavailable() {
        RuntimeException cause = new RuntimeException("timeout");
        FraudAssessmentUnavailableException ex = new FraudAssessmentUnavailableException(cause);
        assertEquals("FRAUD_UNAVAILABLE", ex.getCode());
        assertEquals(ErrorStatus.SERVICE_UNAVAILABLE, ex.getErrorStatus());
        assertSame(cause, ex.getCause());
    }
}
