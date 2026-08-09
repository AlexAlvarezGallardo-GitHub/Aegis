package com.aegis.bff.web.dto;

import java.math.BigDecimal;

/**
 * Request body for depositing funds into a wallet.
 *
 * @param amount    the amount to deposit
 * @param currency  the 3-letter ISO 4217 currency code
 * @param source    the source of the funds (e.g. BANK_TRANSFER, CARD)
 * @param reference an optional external reference for the deposit
 */
public record DepositFundsRequest(BigDecimal amount, String currency, String source, String reference) {
}
