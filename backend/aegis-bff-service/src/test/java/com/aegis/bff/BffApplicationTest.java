package com.aegis.bff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BffApplication")
class BffApplicationTest {

    @Test
    @DisplayName("Should instantiate the application class")
    void shouldInstantiate() {
        // Arrange & Act
        BffApplication app = new BffApplication();

        // Assert
        assertNotNull(app);
    }
}
