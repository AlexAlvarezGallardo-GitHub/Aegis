package com.aegis.fraud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudServiceApplication - Application Entry Point")
class FraudServiceApplicationTest {

    @Test
    @DisplayName("Should instantiate application class")
    void shouldInstantiateApplicationClass() {
        FraudServiceApplication app = new FraudServiceApplication();
        assertNotNull(app);
    }
}
