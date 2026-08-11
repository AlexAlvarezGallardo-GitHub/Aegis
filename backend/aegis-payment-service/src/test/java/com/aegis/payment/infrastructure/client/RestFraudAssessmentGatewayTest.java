package com.aegis.payment.infrastructure.client;

import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.FraudDecision;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.TransactionContext;
import com.aegis.payment.infrastructure.config.FraudProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("RestFraudAssessmentGateway - Fraud Service Adapter")
class RestFraudAssessmentGatewayTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private RestFraudAssessmentGateway gateway;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        FraudProperties properties = new FraudProperties("http://fraud-service", 3000);
        gateway = new RestFraudAssessmentGateway(restClientBuilder, properties);
    }

    private TransactionContext sampleContext() {
        return new TransactionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "EUR"
        );
    }

    @Nested
    @DisplayName("When fraud service returns a decision")
    class WhenFraudServiceReturnsDecision {

        @Test
        @DisplayName("Should map APPROVE to FraudDecision.APPROVE")
        void shouldMapApprove() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andRespond(withSuccess(
                            "{\"decision\": \"APPROVE\"}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.APPROVE, decision);
            mockServer.verify();
        }

        @Test
        @DisplayName("Should map REVIEW to FraudDecision.REVIEW")
        void shouldMapReview() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"decision\": \"REVIEW\"}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.REVIEW, decision);
            mockServer.verify();
        }

        @Test
        @DisplayName("Should map REJECT to FraudDecision.REJECT")
        void shouldMapReject() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"decision\": \"REJECT\"}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.REJECT, decision);
            mockServer.verify();
        }

        @Test
        @DisplayName("Should map unknown decision to REJECT (fail-closed)")
        void shouldMapUnknownToReject() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"decision\": \"UNKNOWN\"}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.REJECT, decision);
            mockServer.verify();
        }

        @Test
        @DisplayName("Should map absent decision to REJECT (fail-closed)")
        void shouldMapAbsentDecisionToReject() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"someOtherField\": \"value\"}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.REJECT, decision);
            mockServer.verify();
        }

        @Test
        @DisplayName("Should map null decision to REJECT (fail-closed)")
        void shouldMapNullDecisionToReject() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"decision\": null}", MediaType.APPLICATION_JSON));

            FraudDecision decision = gateway.assess(sampleContext());

            assertEquals(FraudDecision.REJECT, decision);
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("When fraud service returns an error")
    class WhenFraudServiceReturnsError {

        @Test
        @DisplayName("Should throw FraudAssessmentUnavailableException on 5xx")
        void shouldThrowOn5xx() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());

            assertThrows(FraudAssessmentUnavailableException.class,
                    () -> gateway.assess(sampleContext()));
            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw FraudAssessmentUnavailableException on 4xx")
        void shouldThrowOn4xx() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withBadRequest());

            assertThrows(FraudAssessmentUnavailableException.class,
                    () -> gateway.assess(sampleContext()));
            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw FraudAssessmentUnavailableException on network error")
        void shouldThrowOnNetworkError() {
            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new java.net.ConnectException("Connection refused")));

            assertThrows(FraudAssessmentUnavailableException.class,
                    () -> gateway.assess(sampleContext()));
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Request body correctness")
    class RequestBodyCorrectness {

        @Test
        @DisplayName("Should include transactionType=TRANSFER in request body")
        void shouldIncludeTransactionType() {
            TransactionContext ctx = sampleContext();

            mockServer.expect(requestTo("http://fraud-service/api/v1/fraud/assess"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json("""
                            {
                              "transactionType": "TRANSFER",
                              "transactionId": "%s",
                              "sourceWalletId": "%s",
                              "destWalletId": "%s",
                              "userId": "%s",
                              "amount": 100.00,
                              "currency": "EUR"
                            }
                            """.formatted(
                            ctx.transactionId(),
                            ctx.sourceWalletId(),
                            ctx.destWalletId(),
                            ctx.userId())))
                    .andRespond(withSuccess(
                            "{\"decision\": \"APPROVE\"}", MediaType.APPLICATION_JSON));

            gateway.assess(ctx);
            mockServer.verify();
        }
    }
}
