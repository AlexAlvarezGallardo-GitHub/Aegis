package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new wallet for a user.
 */
public interface CreateWalletUseCase {

    Result execute(Command command);

    record Command(UUID userId, String currency, String correlationId) {}

    record Result(UUID walletId, UUID userId, BigDecimal balance, String currency,
                  String status, Instant createdAt) {}
}
