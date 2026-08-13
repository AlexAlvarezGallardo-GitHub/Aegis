package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.port.outbound.RefundRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link RefundRepository} port.
 */
@Component
public class RefundRepositoryAdapter implements RefundRepository {

    private final RefundJpaRepository jpaRepository;

    public RefundRepositoryAdapter(RefundJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Refund save(Refund refund) {
        jpaRepository.save(new RefundJpaEntity(refund));
        return refund;
    }

    @Override
    public Optional<Refund> findById(UUID refundId) {
        return jpaRepository.findById(refundId).map(RefundJpaEntity::toDomain);
    }

    @Override
    public boolean existsByReference(String reference) {
        return jpaRepository.existsByReference(reference);
    }

    @Override
    public Optional<Refund> findByReference(String reference) {
        return jpaRepository.findByReference(reference).map(RefundJpaEntity::toDomain);
    }
}
