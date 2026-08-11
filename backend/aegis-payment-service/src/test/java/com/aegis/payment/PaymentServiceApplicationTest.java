package com.aegis.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("PaymentServiceApplication - Context")
class PaymentServiceApplicationTest {

    @Test
    @DisplayName("Application class should be instantiable")
    void shouldInstantiate() {
        assertDoesNotThrow(PaymentServiceApplication::new);
    }
}
