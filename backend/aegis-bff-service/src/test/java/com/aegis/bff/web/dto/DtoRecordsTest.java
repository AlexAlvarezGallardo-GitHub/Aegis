package com.aegis.bff.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Records")
class DtoRecordsTest {

    @Test
    @DisplayName("AdjustBalanceRequest should hold amount and description")
    void adjustBalanceRequest() {
        // Arrange & Act
        AdjustBalanceRequest request = new AdjustBalanceRequest(new BigDecimal("50.00"), "bonus");

        // Assert
        assertEquals(new BigDecimal("50.00"), request.amount());
        assertEquals("bonus", request.description());
    }

    @Test
    @DisplayName("AdjustBalanceRequest should allow null description")
    void adjustBalanceRequestNullDescription() {
        // Arrange & Act
        AdjustBalanceRequest request = new AdjustBalanceRequest(new BigDecimal("10.00"), null);

        // Assert
        assertNull(request.description());
    }

    @Test
    @DisplayName("CreateWalletRequest should hold currency")
    void createWalletRequest() {
        // Arrange & Act
        CreateWalletRequest request = new CreateWalletRequest("USD");

        // Assert
        assertEquals("USD", request.currency());
    }

    @Test
    @DisplayName("DepositFundsRequest should hold amount, currency, source and reference")
    void depositFundsRequest() {
        // Arrange & Act
        DepositFundsRequest request = new DepositFundsRequest(
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "ref-123");

        // Assert
        assertEquals(new BigDecimal("100.00"), request.amount());
        assertEquals("EUR", request.currency());
        assertEquals("BANK_TRANSFER", request.source());
        assertEquals("ref-123", request.reference());
    }

    @Test
    @DisplayName("DepositFundsRequest should allow null reference")
    void depositFundsRequestNullReference() {
        // Arrange & Act
        DepositFundsRequest request = new DepositFundsRequest(
                new BigDecimal("25.00"), "EUR", "CARD", null);

        // Assert
        assertNull(request.reference());
    }

    @Test
    @DisplayName("UpdateStatusRequest should hold status")
    void updateStatusRequest() {
        // Arrange & Act
        UpdateStatusRequest request = new UpdateStatusRequest("FROZEN");

        // Assert
        assertEquals("FROZEN", request.status());
    }

    @Test
    @DisplayName("LoginRequest should hold email and password")
    void loginRequest() {
        // Arrange & Act
        LoginRequest request = new LoginRequest("user@test.com", "password123");

        // Assert
        assertEquals("user@test.com", request.email());
        assertEquals("password123", request.password());
    }

    @Test
    @DisplayName("CreateTransferRequest should hold source/dest wallets, amount, currency, description and reference")
    void createTransferRequest() {
        // Arrange
        UUID src = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID dst = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Act
        CreateTransferRequest request = new CreateTransferRequest(
                src, dst, new BigDecimal("25.50"), "USD", "bonus", "ref-123");

        // Assert
        assertEquals(src, request.sourceWalletId());
        assertEquals(dst, request.destWalletId());
        assertEquals(new BigDecimal("25.50"), request.amount());
        assertEquals("USD", request.currency());
        assertEquals("bonus", request.description());
        assertEquals("ref-123", request.reference());
    }

    @Test
    @DisplayName("CreateTransferRequest should allow null description")
    void createTransferRequestNullDescription() {
        // Arrange
        UUID src = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID dst = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Act
        CreateTransferRequest request = new CreateTransferRequest(
                src, dst, new BigDecimal("10.00"), "EUR", null, "ref-456");

        // Assert
        assertNull(request.description());
    }

    @Test
    @DisplayName("PayeeRequest should hold name, id and type")
    void payeeRequest() {
        // Arrange & Act
        PayeeRequest payee = new PayeeRequest("Acme Corp", "acme-001", "MERCHANT");

        // Assert
        assertEquals("Acme Corp", payee.name());
        assertEquals("acme-001", payee.id());
        assertEquals("MERCHANT", payee.type());
    }

    @Test
    @DisplayName("CreatePaymentRequest should hold walletId, amount, currency, payee, description and reference")
    void createPaymentRequest() {
        // Arrange
        UUID walletId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PayeeRequest payee = new PayeeRequest("Store", "store-001", "MERCHANT");

        // Act
        CreatePaymentRequest request = new CreatePaymentRequest(
                walletId, new BigDecimal("50.00"), "USD", payee, "purchase", "PAY-123");

        // Assert
        assertEquals(walletId, request.walletId());
        assertEquals(new BigDecimal("50.00"), request.amount());
        assertEquals("USD", request.currency());
        assertEquals(payee, request.payee());
        assertEquals("purchase", request.description());
        assertEquals("PAY-123", request.reference());
    }

    @Test
    @DisplayName("CreatePaymentRequest should allow null description")
    void createPaymentRequestNullDescription() {
        // Arrange
        UUID walletId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PayeeRequest payee = new PayeeRequest("Store", "store-001", "INDIVIDUAL");

        // Act
        CreatePaymentRequest request = new CreatePaymentRequest(
                walletId, new BigDecimal("25.00"), "EUR", payee, null, "PAY-456");

        // Assert
        assertNull(request.description());
    }
}
