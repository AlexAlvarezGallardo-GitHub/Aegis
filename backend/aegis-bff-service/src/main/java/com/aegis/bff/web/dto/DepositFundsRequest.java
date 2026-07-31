package com.aegis.bff.web.dto;

import java.math.BigDecimal;

/**
 * Request body for depositing funds into a wallet.
 *
 * @param amount    the amount to deposit
 * @param method    the deposit method (e.g. BANK_TRANSFER, CARD)
 * @param reference an optional external reference for the deposit
 */
public record DepositFundsRequest(BigDecimal amount, String method, String reference) {
}
