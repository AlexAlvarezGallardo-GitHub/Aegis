package com.aegis.payment.infrastructure.messaging;

import com.aegis.payment.domain.event.TransferCompleted;
import com.aegis.payment.domain.event.TransferFailed;
import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.payment.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher - Outbox Event Publisher")
class KafkaEventPublisherTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private KafkaEventPublisher publisher;

    @Test
    @DisplayName("Should persist TransferRequested event to outbox")
    void shouldPersistTransferRequested() {
        Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("100.00"), "EUR", "test", "ref-001");
        TransferRequested event = new TransferRequested(transfer);

        publisher.publish(event);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertEquals("TRANSFER", saved.getAggregateType());
        assertEquals(transfer.getId(), saved.getAggregateId());
        assertEquals("TRANSFER_REQUESTED", saved.getEventType());
        assertEquals("PENDING", saved.getStatus());
        assertNotNull(saved.getPayload());
        assertTrue(saved.getPayload().contains("TRANSFER_REQUESTED"));
    }

    @Test
    @DisplayName("Should persist TransferCompleted event to outbox")
    void shouldPersistTransferCompleted() {
        Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("100.00"), "EUR", "test", "ref-001");
        transfer.startFraudCheck();
        transfer.markFundsReserved(UUID.randomUUID());
        transfer.complete();
        TransferCompleted event = new TransferCompleted(transfer);

        publisher.publish(event);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertEquals("TRANSFER", saved.getAggregateType());
        assertEquals(transfer.getId(), saved.getAggregateId());
        assertEquals("TRANSFER_COMPLETED", saved.getEventType());
        assertEquals("PENDING", saved.getStatus());
        assertTrue(saved.getPayload().contains("TRANSFER_COMPLETED"));
    }

    @Test
    @DisplayName("Should persist TransferFailed event to outbox")
    void shouldPersistTransferFailed() {
        Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("100.00"), "EUR", "test", "ref-001");
        transfer.fail("INSUFFICIENT_FUNDS");
        TransferFailed event = new TransferFailed(transfer);

        publisher.publish(event);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertEquals("TRANSFER", saved.getAggregateType());
        assertEquals(transfer.getId(), saved.getAggregateId());
        assertEquals("TRANSFER_FAILED", saved.getEventType());
        assertEquals("PENDING", saved.getStatus());
        assertTrue(saved.getPayload().contains("TRANSFER_FAILED"));
    }

    @Nested
    @DisplayName("When outbox persistence fails")
    class WhenOutboxPersistenceFails {

        @Test
        @DisplayName("Should wrap and rethrow the failure as IllegalStateException")
        void shouldWrapAndRethrow() {
            Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), new BigDecimal("100.00"), "EUR", "test", "ref-001");
            TransferRequested event = new TransferRequested(transfer);

            org.mockito.Mockito.when(outboxRepository.save(any(OutboxEventJpaEntity.class)))
                    .thenThrow(new RuntimeException("DB down"));

            assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        }
    }
}
