package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.port.outbound.TransferRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link TransferRepository} port.
 */
@Component
public class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;

    public TransferRepositoryAdapter(TransferJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        jpaRepository.save(new TransferJpaEntity(transfer));
        return transfer;
    }

    @Override
    public Optional<Transfer> findById(UUID transferId) {
        return jpaRepository.findById(transferId).map(TransferJpaEntity::toDomain);
    }

    @Override
    public boolean existsBySourceWalletIdAndReference(UUID sourceWalletId, String reference) {
        return jpaRepository.existsBySourceWalletIdAndReference(sourceWalletId, reference);
    }
}
