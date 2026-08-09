package com.aegis.fraud.infrastructure.messaging;

import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.infrastructure.persistence.ProcessedEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionEventConsumer - Kafka Consumer")
class TransactionEventConsumerTest {

    @Mock
    private AssessFraudUseCase assessFraudUseCase;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionEventConsumer(assessFraudUseCase, processedEventRepository);
    }

    @Nested
    @DisplayName("When receiving TransferRequestedEvent")
    class WhenReceivingTransferRequestedEvent {

        @Test
        @DisplayName("Should call assess with correct command")
        void shouldCallAssessWithCorrectCommand() {
            UUID eventId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            TransactionEventConsumer.TransferRequestedEvent event =
                    new TransactionEventConsumer.TransferRequestedEvent(
                            eventId, transactionId, new BigDecimal("1500.50"), "EUR",
                            sourceWalletId, destWalletId, userId);

            FraudAssessment mockAssessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), transactionId, "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), java.time.Instant.now());
            when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                    .thenReturn(1);
            when(assessFraudUseCase.assess(any(AssessFraudUseCase.AssessmentCommand.class)))
                    .thenReturn(mockAssessment);

            consumer.onTransferRequested(event, "payment.transfer.requested", 0, 12L);

            ArgumentCaptor<AssessFraudUseCase.AssessmentCommand> captor =
                    ArgumentCaptor.forClass(AssessFraudUseCase.AssessmentCommand.class);
            verify(assessFraudUseCase).assess(captor.capture());

            AssessFraudUseCase.AssessmentCommand command = captor.getValue();
            assertEquals(transactionId, command.transactionId());
            assertEquals("TRANSFER", command.transactionType());
            assertEquals(new BigDecimal("1500.50"), command.amount());
            assertEquals("EUR", command.currency());
            assertEquals(sourceWalletId, command.sourceWalletId());
            assertEquals(destWalletId, command.destWalletId());
            assertEquals(userId, command.userId());
            assertNull(command.countryCode());
            verify(processedEventRepository).insertIfAbsent(eq(eventId),
                    eq("payment.transfer.requested"), eq(0), eq(12L), any());
        }

        @Test
        @DisplayName("Should pass null countryCode")
        void shouldPassNullCountryCode() {
            TransactionEventConsumer.TransferRequestedEvent event =
                    new TransactionEventConsumer.TransferRequestedEvent(
                            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "EUR",
                            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            when(processedEventRepository.insertIfAbsent(any(), any(), anyInt(), anyLong(), any()))
                    .thenReturn(1);
            when(assessFraudUseCase.assess(any())).thenReturn(
                    FraudAssessment.rehydrate(UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                            FraudDecision.APPROVE, List.of(), java.time.Instant.now()));

            consumer.onTransferRequested(event, "payment.transfer.requested", 0, 12L);

            ArgumentCaptor<AssessFraudUseCase.AssessmentCommand> captor =
                    ArgumentCaptor.forClass(AssessFraudUseCase.AssessmentCommand.class);
            verify(assessFraudUseCase).assess(captor.capture());
            assertNull(captor.getValue().countryCode());
        }

        @Test
        @DisplayName("Should skip already-processed events without assessing twice")
        void shouldSkipDuplicateEvents() {
            UUID eventId = UUID.randomUUID();
            TransactionEventConsumer.TransferRequestedEvent event =
                    new TransactionEventConsumer.TransferRequestedEvent(
                            eventId, UUID.randomUUID(), BigDecimal.TEN, "EUR",
                            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                    .thenReturn(0);

            consumer.onTransferRequested(event, "payment.transfer.requested", 3, 77L);

            verify(assessFraudUseCase, never()).assess(any());
            verify(processedEventRepository).insertIfAbsent(eq(eventId),
                    eq("payment.transfer.requested"), eq(3), eq(77L), any());
        }
    }

    @Nested
    @DisplayName("TransferRequestedEvent record")
    class TransferRequestedEventRecord {

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFieldsCorrectly() {
            UUID eventId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            TransactionEventConsumer.TransferRequestedEvent event =
                    new TransactionEventConsumer.TransferRequestedEvent(
                            eventId, transactionId, new BigDecimal("2000.00"), "USD",
                            sourceWalletId, destWalletId, userId);

            assertEquals(eventId, event.eventId());
            assertEquals(transactionId, event.transactionId());
            assertEquals(new BigDecimal("2000.00"), event.amount());
            assertEquals("USD", event.currency());
            assertEquals(sourceWalletId, event.sourceWalletId());
            assertEquals(destWalletId, event.destWalletId());
            assertEquals(userId, event.userId());
        }
    }
}
