package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRepositoryAdapter - Persistence Adapter")
class PaymentRepositoryAdapterTest {

    @Mock private PaymentJpaRepository jpaRepository;
    @InjectMocks private PaymentRepositoryAdapter adapter;

    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    @Test
    @DisplayName("Should save domain payment via JPA entity")
    void shouldSave() {
        Payment payment = Payment.request(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001");
        when(jpaRepository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = adapter.save(payment);

        assertEquals(payment.getId(), result.getId());
        verify(jpaRepository).save(any(PaymentJpaEntity.class));
    }

    @Test
    @DisplayName("Should find payment by ID and map to domain")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        UUID wallet = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Instant now = Instant.now();

        Payment domain = Payment.rehydrate(id, wallet, user,
                new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001",
                PaymentStatus.PENDING, null, null, null, now, now, null);
        PaymentJpaEntity entity = new PaymentJpaEntity(domain);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Payment> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(PaymentStatus.PENDING, result.get().getStatus());
    }

    @Test
    @DisplayName("Should return empty when not found")
    void shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Payment> result = adapter.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should check existence by wallet and reference")
    void shouldCheckExistence() {
        UUID wallet = UUID.randomUUID();
        when(jpaRepository.existsByWalletIdAndReference(wallet, "PAY-001")).thenReturn(true);

        assertTrue(adapter.existsByWalletIdAndReference(wallet, "PAY-001"));
    }
}
