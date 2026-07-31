package com.aegis.bff.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Provides a pre-configured {@link RestClient.Builder} with connection and read timeouts.
 *
 * <p>RestClient's default error handling propagates 4xx/5xx responses as
 * {@link org.springframework.web.client.RestClientResponseException}, which the
 * {@link com.aegis.bff.web.advice.BffExceptionHandler} translates into the standard error envelope.</p>
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());

        return RestClient.builder().requestFactory(factory);
    }
}
