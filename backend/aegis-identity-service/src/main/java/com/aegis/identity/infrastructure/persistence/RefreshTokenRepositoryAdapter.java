package com.aegis.identity.infrastructure.persistence;

import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.port.outbound.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter for the {@link RefreshTokenRepository} port.
 */
@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public void save(StoredRefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(
                token.id(),
                token.tokenHash(),
                token.userId().value(),
                token.expiresAt(),
                token.createdAt()
        );
        jpaRepository.save(entity);
    }

    @Override
    public void revoke(String tokenHash) {
        jpaRepository.findByTokenHash(tokenHash).ifPresent(entity -> {
            entity.revoke();
            jpaRepository.save(entity);
        });
    }

    private StoredRefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new StoredRefreshToken(
                entity.getId(),
                entity.getTokenHash(),
                UserId.of(entity.getUserId()),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
