package com.aegis.payment.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Provides pre-configured {@link RestClient.Builder} instances with connection and
 * read timeouts sized for the fraud service's 200 ms budget plus headroom, and for
 * the wallet service's hold/settle round trips.
 */
@Configuration
@EnableConfigurationProperties({FraudProperties.class, WalletProperties.class})
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 2000;

    @Bean
    public RestClient.Builder restClientBuilder(FraudProperties fraudProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(fraudProperties.timeoutMs());

        return RestClient.builder().requestFactory(factory);
    }
}
