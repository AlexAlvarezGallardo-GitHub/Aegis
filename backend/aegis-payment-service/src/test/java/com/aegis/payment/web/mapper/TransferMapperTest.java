package com.aegis.payment.web.mapper;

import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.web.dto.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferMapper - Web to Domain Mapping")
class TransferMapperTest {

    @Test
    @DisplayName("Should map TransferRequest to TransferCommand")
    void shouldMapRequestToCommand() {
        UUID source = UUID.randomUUID();
        UUID dest = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        TransferRequest request = new TransferRequest(
                source, dest, user, new BigDecimal("100.00"), "EUR", "desc", "ref-001");

        TransferFundsUseCase.TransferCommand command = TransferMapper.toCommand(request);

        assertEquals(source, command.sourceWalletId());
        assertEquals(dest, command.destWalletId());
        assertEquals(user, command.userId());
        assertEquals(new BigDecimal("100.00"), command.amount());
        assertEquals("EUR", command.currency());
        assertEquals("desc", command.description());
        assertEquals("ref-001", command.reference());
    }

    @Test
    @DisplayName("Should map null description correctly")
    void shouldMapNullDescription() {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "USD", null, "ref-002");

        TransferFundsUseCase.TransferCommand command = TransferMapper.toCommand(request);

        assertNull(command.description());
    }
}
