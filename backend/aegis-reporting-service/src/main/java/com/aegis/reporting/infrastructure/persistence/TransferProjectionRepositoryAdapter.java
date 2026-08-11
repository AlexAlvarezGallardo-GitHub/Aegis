package com.aegis.reporting.infrastructure.persistence;

import com.aegis.reporting.domain.model.TransferProjection;
import com.aegis.reporting.domain.port.outbound.TransferProjectionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter that bridges the domain {@link TransferProjectionRepository} port
 * with the Spring Data JPA {@link TransferProjectionJpaRepository}.
 */
@Repository
public class TransferProjectionRepositoryAdapter implements TransferProjectionRepository {

    private final TransferProjectionJpaRepository jpaRepository;

    public TransferProjectionRepositoryAdapter(TransferProjectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TransferProjection save(TransferProjection projection) {
        TransferProjectionJpaEntity entity = toEntity(projection);
        TransferProjectionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TransferProjection> findByTransferId(UUID transferId) {
        return jpaRepository.findByTransferId(transferId).map(this::toDomain);
    }

    private TransferProjectionJpaEntity toEntity(TransferProjection projection) {
        return new TransferProjectionJpaEntity(
                projection.id(),
                projection.transferId(),
                projection.sourceWalletId(),
                projection.destWalletId(),
                projection.userId(),
                projection.amount(),
                projection.currency(),
                projection.status(),
                projection.failureReason(),
                projection.eventTimestamp()
        );
    }

    private TransferProjection toDomain(TransferProjectionJpaEntity entity) {
        return new TransferProjection(
                entity.getId(),
                entity.getTransferId(),
                entity.getSourceWalletId(),
                entity.getDestWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getEventTimestamp()
        );
    }
}
