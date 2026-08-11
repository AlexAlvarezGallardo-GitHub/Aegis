package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.domain.model.LedgerEntry;
import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;
    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    public WalletRepositoryAdapter(WalletJpaRepository walletJpaRepository,
                                    LedgerEntryJpaRepository ledgerEntryJpaRepository) {
        this.walletJpaRepository = walletJpaRepository;
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = toEntity(wallet);
        WalletJpaEntity saved = walletJpaRepository.save(entity);
        saveLedgerEntries(wallet);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(WalletId walletId) {
        return walletJpaRepository.findById(walletId.value())
                .map(this::toDomainWithEntries);
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return walletJpaRepository.findByUserId(userId).stream()
                .map(this::toDomainWithEntries)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(UUID userId) {
        return walletJpaRepository.countByUserId(userId);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
        return walletJpaRepository.findByIdForUpdate(walletId.value())
                .map(this::toDomainWithEntries);
    }

    private void saveLedgerEntries(Wallet wallet) {
        var existingIds = ledgerEntryJpaRepository
                .findByWalletIdOrderByCreatedAtAsc(wallet.getWalletId().value()).stream()
                .map(LedgerEntryJpaEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        for (LedgerEntry entry : wallet.getLedgerEntries()) {
            if (!existingIds.contains(entry.id())) {
                ledgerEntryJpaRepository.save(new LedgerEntryJpaEntity(
                        entry.id(),
                        entry.walletId(),
                        entry.type().name(),
                        entry.amount(),
                        entry.currency(),
                        entry.reference(),
                        entry.reversalOf(),
                        entry.timestamp()
                ));
            }
        }
    }

    private WalletJpaEntity toEntity(Wallet wallet) {
        return new WalletJpaEntity(
                wallet.getWalletId().value(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt(),
                wallet.getVersion()
        );
    }

    private Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.rehydrate(
                WalletId.of(entity.getId()),
                entity.getUserId(),
                entity.getBalance(),
                entity.getCurrency(),
                WalletStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                List.of()
        );
    }

    private Wallet toDomainWithEntries(WalletJpaEntity entity) {
        List<LedgerEntry> entries = ledgerEntryJpaRepository
                .findByWalletIdOrderByCreatedAtAsc(entity.getId()).stream()
                .map(e -> new LedgerEntry(
                        e.getId(),
                        e.getWalletId(),
                        LedgerEntryType.valueOf(e.getType()),
                        e.getAmount(),
                        e.getCurrency(),
                        e.getReference(),
                        e.getCreatedAt(),
                        e.getReversalOf()
                ))
                .collect(Collectors.toList());

        return Wallet.rehydrate(
                WalletId.of(entity.getId()),
                entity.getUserId(),
                entity.getBalance(),
                entity.getCurrency(),
                WalletStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entries
        );
    }
}
