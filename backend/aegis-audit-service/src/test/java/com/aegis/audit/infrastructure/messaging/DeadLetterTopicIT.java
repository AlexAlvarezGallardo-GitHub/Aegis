package com.aegis.audit.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the dead letter topic (DLT) behaviour of the audit service consumers.
 *
 * <p>A record that cannot be deserialized into {@code FundsDepositedEvent} is
 * retried by the {@code DefaultErrorHandler} and, once the attempts are exhausted,
 * published to the topic's dead letter topic ({@code wallet.funds.deposited.dlt}).</p>
 */
@SpringBootTest
@Testcontainers
@DisplayName("Audit Kafka DLT - Poison Message Handling")
class DeadLetterTopicIT {

    private static final String SOURCE_TOPIC = "wallet.funds.deposited";
    private static final String DLT_TOPIC = SOURCE_TOPIC + ".dlt";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_audit_dlt")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("aegis.kafka.retry.max-attempts", () -> "2");
        registry.add("aegis.kafka.retry.backoff-ms", () -> "100");
    }

    @Test
    @DisplayName("Poison message should be routed to the dead letter topic after retries")
    void poisonMessageShouldReachDeadLetterTopic() throws Exception {
        // Arrange - publish a malformed payload (valid JSON, wrong structure) to the source topic
        String bootstrapServers = kafka.getBootstrapServers();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps(bootstrapServers))) {
            producer.send(new ProducerRecord<>(SOURCE_TOPIC, "poison-key-1", "{\"unexpected\":\"structure\"}")).get();
        }

        // Act - consume from the DLT and wait for the recovered record
        ConsumerRecords<String, String> records = pollUntilNonEmpty(DLT_TOPIC, bootstrapServers);

        // Assert
        assertNotNull(records, "A poison message should be published to the DLT within 20 seconds");
        assertFalse(records.isEmpty(), "The dead letter topic should contain the recovered poison message");
    }

    private ConsumerRecords<String, String> pollUntilNonEmpty(String topic, String bootstrapServers) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps(bootstrapServers))) {
            consumer.subscribe(Collections.singletonList(topic));
            long deadline = System.currentTimeMillis() + 20_000;
            ConsumerRecords<String, String> result = null;
            while (System.currentTimeMillis() < deadline && result == null) {
                var polled = consumer.poll(Duration.ofMillis(1000));
                if (!polled.isEmpty()) {
                    result = polled;
                }
            }
            return result;
        }
    }

    private Properties producerProps(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private Properties consumerProps(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-verify-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
