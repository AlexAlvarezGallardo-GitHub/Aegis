package com.aegis.wallet.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds the {@code aegis.kafka.topics} configuration map (event type → Kafka topic).
 * <p>
 * Topic names live in {@code application.yml} so they can be managed without code changes.
 * </p>
 */
@ConfigurationProperties(prefix = "aegis.kafka")
public class KafkaTopicsProperties {

    private Map<String, String> topics = new HashMap<>();

    public Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }

    /**
     * Resolves the Kafka topic for the given domain event type.
     *
     * @param eventType the domain event type (e.g. {@code FUNDS_DEPOSITED})
     * @return the configured topic name, or {@code null} if not configured
     */
    public String topicFor(String eventType) {
        return topics.get(eventType);
    }
}
