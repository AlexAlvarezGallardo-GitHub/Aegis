package com.aegis.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {

    List<WalletJpaEntity> findByUserId(UUID userId);

    long countByUserId(UUID userId);
}
