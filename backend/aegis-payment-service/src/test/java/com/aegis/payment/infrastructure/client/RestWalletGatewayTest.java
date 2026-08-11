package com.aegis.payment.infrastructure.client;

import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import com.aegis.payment.infrastructure.config.WalletProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@DisplayName("RestWalletGateway - Saga Hold/Settle/Release Adapter")
class RestWalletGatewayTest {

    private static final String BASE_URL = "http://wallet.test";

    private MockRestServiceServer mockServer;
    private RestWalletGateway gateway;

    @BeforeEach
    void setUp() {
        // bindTo(RestClient.Builder) intercepts the builder the gateway uses to
        // construct its own RestClient, so no instance sharing is needed.
        org.springframework.web.client.RestClient.Builder builder =
                org.springframework.web.client.RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gateway = new RestWalletGateway(builder, new WalletProperties(BASE_URL, 3000));
    }

    @Nested
    @DisplayName("createHold")
    class CreateHold {

        @Test
        @DisplayName("Should return hold id on 201 with holdId in response")
        void shouldReturnHoldId() {
            UUID walletId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/" + walletId + "/holds"))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{\"holdId\":\"" + holdId + "\",\"status\":\"ACTIVE\"}", org.springframework.http.MediaType.APPLICATION_JSON));

            UUID result = gateway.createHold(walletId, new BigDecimal("50.00"), "EUR", "ref-1");

            assertEquals(holdId, result);
        }

        @Test
        @DisplayName("Should throw SettlementFailedException when wallet rejects with 422")
        void shouldThrowOnReject() {
            UUID walletId = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/" + walletId + "/holds"))
                    .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body("{\"code\":\"INSUFFICIENT_FUNDS\"}"));

            assertThrows(SettlementFailedException.class,
                    () -> gateway.createHold(walletId, new BigDecimal("50.00"), "EUR", "ref-1"));
        }

        @Test
        @DisplayName("Should throw SettlementFailedException when response has no holdId")
        void shouldThrowWhenMissingHoldId() {
            UUID walletId = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/" + walletId + "/holds"))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{}", org.springframework.http.MediaType.APPLICATION_JSON));

            assertThrows(SettlementFailedException.class,
                    () -> gateway.createHold(walletId, new BigDecimal("50.00"), "EUR", "ref-1"));
        }
    }

    @Nested
    @DisplayName("settle")
    class Settle {

        @Test
        @DisplayName("Should return settlement result on success")
        void shouldReturnResult() {
            UUID transferId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/transfers/settle"))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{\"sourceNewBalance\":150.00,\"destNewBalance\":230.00}",
                            org.springframework.http.MediaType.APPLICATION_JSON));

            WalletGateway.SettlementResult result =
                    gateway.settle(transferId, holdId, source, dest, new BigDecimal("50.00"), "EUR");

            assertEquals(0, new BigDecimal("150.00").compareTo(result.sourceNewBalance()));
            assertEquals(0, new BigDecimal("230.00").compareTo(result.destNewBalance()));
        }

        @Test
        @DisplayName("Should throw SettlementFailedException when settlement rejected")
        void shouldThrowOnReject() {
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/transfers/settle"))
                    .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT));

            assertThrows(SettlementFailedException.class,
                    () -> gateway.settle(UUID.randomUUID(), UUID.randomUUID(),
                            UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), "EUR"));
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("Should not throw on successful release")
        void shouldRelease() {
            UUID walletId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/" + walletId + "/holds/" + holdId + "/release"))
                    .andRespond(MockRestResponseCreators.withSuccess());

            assertDoesNotThrow(() -> gateway.release(walletId, holdId));
        }

        @Test
        @DisplayName("Should throw SettlementFailedException when release rejected")
        void shouldThrowOnReject() {
            UUID walletId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            mockServer.expect(requestTo(BASE_URL + "/api/v1/wallets/" + walletId + "/holds/" + holdId + "/release"))
                    .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT));

            assertThrows(SettlementFailedException.class,
                    () -> gateway.release(walletId, holdId));
        }
    }
}
