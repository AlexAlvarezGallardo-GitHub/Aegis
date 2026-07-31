package com.aegis.fraud.web.controller;

import com.aegis.fraud.application.dto.AssessmentResponse;
import com.aegis.fraud.application.mapper.AssessmentMapper;
import com.aegis.fraud.domain.exception.AssessmentNotFoundException;
import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.RuleEvaluation;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.web.dto.AssessmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudController - REST Controller")
class FraudControllerTest {

    @Mock
    private AssessFraudUseCase assessFraudUseCase;

    private FraudController controller;

    @BeforeEach
    void setUp() {
        controller = new FraudController(assessFraudUseCase);
    }

    @Nested
    @DisplayName("When assessing fraud via POST /assess")
    class WhenAssessingFraud {

        @Test
        @DisplayName("Should return 200 with assessment response")
        void shouldReturn200WithAssessmentResponse() {
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 30,
                    FraudDecision.REVIEW,
                    List.of(new RuleEvaluation("AMOUNT", 30, true, "exceeds")),
                    Instant.now());

            when(assessFraudUseCase.assess(any())).thenReturn(assessment);

            AssessmentRequest request = new AssessmentRequest(
                    transactionId, "TRANSFER", new BigDecimal("1500.50"), "EUR",
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ES");

            ResponseEntity<AssessmentResponse> response = controller.assess(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(assessmentId, response.getBody().assessmentId());
            assertEquals(transactionId, response.getBody().transactionId());
            assertEquals("TRANSFER", response.getBody().transactionType());
            assertEquals(30, response.getBody().riskScore());
            assertEquals("REVIEW", response.getBody().decision());

            verify(assessFraudUseCase).assess(any());
        }

        @Test
        @DisplayName("Should map request to use case command correctly")
        void shouldMapRequestToUseCaseCommandCorrectly() {
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), transactionId, "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());
            when(assessFraudUseCase.assess(any())).thenReturn(assessment);

            AssessmentRequest request = new AssessmentRequest(
                    transactionId, "TRANSFER", new BigDecimal("100.00"), "EUR",
                    sourceWalletId, destWalletId, userId, "ES");

            controller.assess(request);

            verify(assessFraudUseCase).assess(argThat(cmd ->
                    cmd.transactionId().equals(transactionId) &&
                    cmd.transactionType().equals("TRANSFER") &&
                    cmd.amount().compareTo(new BigDecimal("100.00")) == 0 &&
                    cmd.currency().equals("EUR") &&
                    cmd.sourceWalletId().equals(sourceWalletId) &&
                    cmd.destWalletId().equals(destWalletId) &&
                    cmd.userId().equals(userId) &&
                    cmd.countryCode().equals("ES")));
        }
    }

    @Nested
    @DisplayName("When getting assessment via GET /assessments/{id}")
    class WhenGettingAssessment {

        @Test
        @DisplayName("Should return 200 with assessment response")
        void shouldReturn200WithAssessmentResponse() {
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());

            when(assessFraudUseCase.findById(assessmentId)).thenReturn(assessment);

            ResponseEntity<AssessmentResponse> response = controller.getAssessment(assessmentId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(assessmentId, response.getBody().assessmentId());
            assertEquals("APPROVE", response.getBody().decision());

            verify(assessFraudUseCase).findById(assessmentId);
        }

        @Test
        @DisplayName("Should propagate AssessmentNotFoundException")
        void shouldPropagateAssessmentNotFoundException() {
            UUID assessmentId = UUID.randomUUID();
            when(assessFraudUseCase.findById(assessmentId))
                    .thenThrow(new AssessmentNotFoundException(assessmentId));

            assertThrows(AssessmentNotFoundException.class,
                    () -> controller.getAssessment(assessmentId));
        }
    }
}
