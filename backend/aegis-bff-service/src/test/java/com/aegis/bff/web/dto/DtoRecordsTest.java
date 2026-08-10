package com.aegis.bff.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
