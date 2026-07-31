package com.aegis.bff.web.dto;

/**
 * Request body for creating a new wallet.
 *
 * @param currency the ISO-4217 currency code for the wallet
 */
public record CreateWalletRequest(String currency) {
}
