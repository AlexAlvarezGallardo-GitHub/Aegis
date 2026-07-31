package com.aegis.fraud.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KafkaTopicsProperties - Topic Resolution")
class KafkaTopicsPropertiesTest {

    @Nested
    @DisplayName("When resolving topic for event type")
    class WhenResolvingTopicForEventType {

        @Test
        @DisplayName("Should return configured topic for known event type")
        void shouldReturnConfiguredTopicForKnownEventType() {
            KafkaTopicsProperties properties = new KafkaTopicsProperties();
            Map<String, String> topics = new HashMap<>();
            topics.put("FRAUD_ASSESSMENT_COMPLETED", "aegis.fraud.assessment-completed");
            properties.setTopics(topics);

            String topic = properties.topicFor("FRAUD_ASSESSMENT_COMPLETED");

            assertEquals("aegis.fraud.assessment-completed", topic);
        }

        @Test
        @DisplayName("Should return null for unknown event type")
        void shouldReturnNullForUnknownEventType() {
            KafkaTopicsProperties properties = new KafkaTopicsProperties();
            Map<String, String> topics = new HashMap<>();
            topics.put("FRAUD_ASSESSMENT_COMPLETED", "aegis.fraud.assessment-completed");
            properties.setTopics(topics);

            String topic = properties.topicFor("UNKNOWN_EVENT_TYPE");

            assertNull(topic);
        }

        @Test
        @DisplayName("Should return null when topics map is empty")
        void shouldReturnNullWhenTopicsMapIsEmpty() {
            KafkaTopicsProperties properties = new KafkaTopicsProperties();

            String topic = properties.topicFor("FRAUD_ASSESSMENT_COMPLETED");

            assertNull(topic);
        }

        @Test
        @DisplayName("Should return empty map by default")
        void shouldReturnEmptyMapByDefault() {
            KafkaTopicsProperties properties = new KafkaTopicsProperties();

            assertNotNull(properties.getTopics());
            assertTrue(properties.getTopics().isEmpty());
        }

        @Test
        @DisplayName("Should allow setting multiple topics")
        void shouldAllowSettingMultipleTopics() {
            KafkaTopicsProperties properties = new KafkaTopicsProperties();
            Map<String, String> topics = new HashMap<>();
            topics.put("FRAUD_ASSESSMENT_COMPLETED", "aegis.fraud.assessment-completed");
            topics.put("USER_REGISTERED", "aegis.identity.user-registered");
            properties.setTopics(topics);

            assertEquals("aegis.fraud.assessment-completed", properties.topicFor("FRAUD_ASSESSMENT_COMPLETED"));
            assertEquals("aegis.identity.user-registered", properties.topicFor("USER_REGISTERED"));
            assertEquals(2, properties.getTopics().size());
        }
    }
}
