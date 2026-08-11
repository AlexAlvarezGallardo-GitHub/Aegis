package com.aegis.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wallet service connection properties for the transfer saga.
 *
 * @param baseUrl  base URL of the wallet service
 * @param timeoutMs read timeout for wallet REST calls
 */
@ConfigurationProperties(prefix = "aegis.payment.wallet")
public record WalletProperties(String baseUrl, int timeoutMs) {
}
