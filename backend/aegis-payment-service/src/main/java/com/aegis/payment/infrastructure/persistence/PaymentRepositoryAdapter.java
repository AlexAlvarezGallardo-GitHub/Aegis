package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.port.outbound.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link PaymentRepository} port.
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        jpaRepository.save(new PaymentJpaEntity(payment));
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId).map(PaymentJpaEntity::toDomain);
    }

    @Override
    public boolean existsByWalletIdAndReference(UUID walletId, String reference) {
        return jpaRepository.existsByWalletIdAndReference(walletId, reference);
    }
}
