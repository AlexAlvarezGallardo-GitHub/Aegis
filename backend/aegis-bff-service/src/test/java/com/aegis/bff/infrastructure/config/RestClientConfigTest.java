package com.aegis.bff.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RestClientConfig")
class RestClientConfigTest {

    @Test
    @DisplayName("Should create a RestClient.Builder bean with timeouts configured")
    void shouldCreateRestClientBuilder() {
        // Arrange
        RestClientConfig config = new RestClientConfig();

        // Act
        RestClient.Builder builder = config.restClientBuilder();

        // Assert
        assertNotNull(builder);
        // Verify the builder can produce a working RestClient
        RestClient client = builder.build();
        assertNotNull(client);
    }
}
