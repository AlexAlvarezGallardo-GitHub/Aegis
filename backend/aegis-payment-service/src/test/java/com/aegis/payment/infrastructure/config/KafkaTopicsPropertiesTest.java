package com.aegis.payment.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KafkaTopicsProperties - Topic Resolution")
class KafkaTopicsPropertiesTest {

    @Test
    @DisplayName("Should resolve topic for known event type")
    void shouldResolveTopic() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        props.setTopics(Map.of(
                "TRANSFER_REQUESTED", "payment.transfer.requested",
                "TRANSFER_COMPLETED", "payment.transfer.completed",
                "TRANSFER_FAILED", "payment.transfer.failed"
        ));

        assertEquals("payment.transfer.requested", props.topicFor("TRANSFER_REQUESTED"));
        assertEquals("payment.transfer.completed", props.topicFor("TRANSFER_COMPLETED"));
        assertEquals("payment.transfer.failed", props.topicFor("TRANSFER_FAILED"));
    }

    @Test
    @DisplayName("Should return null for unknown event type")
    void shouldReturnNullForUnknown() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        props.setTopics(Map.of("TRANSFER_REQUESTED", "payment.transfer.requested"));

        assertNull(props.topicFor("UNKNOWN_EVENT"));
    }

    @Test
    @DisplayName("Should return empty topics map by default")
    void shouldHaveEmptyTopicsByDefault() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        assertNotNull(props.getTopics());
        assertTrue(props.getTopics().isEmpty());
    }
}
