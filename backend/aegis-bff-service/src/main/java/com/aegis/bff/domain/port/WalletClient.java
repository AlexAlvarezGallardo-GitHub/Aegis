package com.aegis.bff.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Outbound port for communicating with the Wallet Service.
 */
public interface WalletClient {

    /**
     * Creates a new wallet for the given user.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id
     * @param currency      the wallet currency
     * @param correlationId the correlation id for tracing
     * @return the created wallet
     */
    JsonNode createWallet(String accessToken, String userId, String currency, String correlationId);

    /**
     * Lists all wallets for the given user.
     *
     * @param accessToken the bearer token
     * @param userId      the user id
     * @return the list of wallets
     */
    JsonNode listWallets(String accessToken, String userId);

    /**
     * Gets a specific wallet by id.
     *
     * @param accessToken the bearer token
     * @param userId      the user id
     * @param walletId    the wallet id
     * @return the wallet
     */
    JsonNode getWallet(String accessToken, String userId, String walletId);

    /**
     * Adjusts the balance of a wallet.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id
     * @param walletId      the wallet id
     * @param amount        the amount (positive for credit, negative for debit)
     * @param description   an optional description for the adjustment
     * @param correlationId the correlation id for tracing
     * @return the adjusted wallet
     */
    JsonNode adjustBalance(String accessToken, String userId, String walletId,
                           java.math.BigDecimal amount, String description,
                           String correlationId);

    /**
     * Deposits funds into a wallet.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id
     * @param walletId      the wallet id
     * @param amount        the amount to deposit
     * @param currency      the 3-letter ISO 4217 currency code
     * @param source        the source of the funds
     * @param reference     the external reference
     * @param correlationId the correlation id for tracing
     * @return the deposit result
     */
    JsonNode depositFunds(String accessToken, String userId, String walletId,
                          java.math.BigDecimal amount, String currency, String source,
                          String reference, String correlationId);

    /**
     * Updates the status of a wallet.
     *
     * @param accessToken the bearer token
     * @param userId      the user id
     * @param walletId    the wallet id
     * @param status      the new status
     * @return the updated wallet
     */
    JsonNode updateStatus(String accessToken, String userId, String walletId, String status);
}
