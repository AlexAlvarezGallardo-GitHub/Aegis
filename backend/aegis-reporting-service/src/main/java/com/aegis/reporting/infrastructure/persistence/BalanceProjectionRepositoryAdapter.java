package com.aegis.reporting.infrastructure.persistence;

import com.aegis.reporting.domain.model.BalanceProjection;
import com.aegis.reporting.domain.port.outbound.BalanceProjectionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter that bridges the domain {@link BalanceProjectionRepository} port
 * with the Spring Data JPA {@link BalanceProjectionJpaRepository}.
 */
@Repository
public class BalanceProjectionRepositoryAdapter implements BalanceProjectionRepository {

    private final BalanceProjectionJpaRepository jpaRepository;

    public BalanceProjectionRepositoryAdapter(BalanceProjectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BalanceProjection save(BalanceProjection projection) {
        BalanceProjectionJpaEntity entity = toEntity(projection);
        BalanceProjectionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BalanceProjection> findByWalletId(UUID walletId) {
        return jpaRepository.findByWalletId(walletId).map(this::toDomain);
    }

    private BalanceProjectionJpaEntity toEntity(BalanceProjection projection) {
        return new BalanceProjectionJpaEntity(
                projection.id(),
                projection.walletId(),
                projection.userId(),
                projection.balance(),
                projection.currency(),
                projection.lastUpdated()
        );
    }

    private BalanceProjection toDomain(BalanceProjectionJpaEntity entity) {
        return new BalanceProjection(
                entity.getId(),
                entity.getWalletId(),
                entity.getUserId(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getLastUpdated()
        );
    }
}
