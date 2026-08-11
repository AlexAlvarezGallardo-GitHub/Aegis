package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HoldRepositoryAdapter implements HoldRepository {

    private final HoldJpaRepository holdJpaRepository;

    public HoldRepositoryAdapter(HoldJpaRepository holdJpaRepository) {
        this.holdJpaRepository = holdJpaRepository;
    }

    @Override
    public Hold save(Hold hold) {
        HoldJpaEntity entity = toEntity(hold);
        HoldJpaEntity saved = holdJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Hold> findById(UUID holdId) {
        return holdJpaRepository.findById(holdId).map(this::toDomain);
    }

    @Override
    public Optional<Hold> findActiveByReference(String reference) {
        return holdJpaRepository.findActiveByReference(reference).map(this::toDomain);
    }

    @Override
    public BigDecimal sumActiveAmountByWalletId(UUID walletId) {
        BigDecimal sum = holdJpaRepository.sumActiveAmountByWalletId(walletId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private HoldJpaEntity toEntity(Hold hold) {
        return new HoldJpaEntity(
                hold.getId(),
                hold.getWalletId(),
                hold.getAmount(),
                hold.getCurrency(),
                hold.getReference(),
                hold.getStatus().name(),
                hold.getCreatedAt(),
                hold.getExpiresAt()
        );
    }

    private Hold toDomain(HoldJpaEntity entity) {
        return Hold.rehydrate(
                entity.getId(),
                entity.getWalletId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getReference(),
                HoldStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
