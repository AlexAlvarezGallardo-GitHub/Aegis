package com.aegis.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka infrastructure configuration — consumer factory, listener container,
 * producer template and retry/DLT error handling.
 */
@Configuration
public class KafkaConfig {

    @Value("${aegis.kafka.retry.max-attempts:3}")
    private long maxAttempts;

    @Value("${aegis.kafka.retry.backoff-ms:1000}")
    private long backoffMs;

    @Value("${aegis.kafka.dlt-suffix:.dlt}")
    private String dltSuffix;

    /**
     * Consumer factory with trusted packages for the payment service.
     *
     * @param properties the Spring Boot Kafka properties
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties properties) {
        var props = properties.buildConsumerProperties();
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.aegis.payment.*,java.util,java.time");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory with retry + DLT error handling.
     *
     * @param consumerFactory the consumer factory
     * @param kafkaTemplate   the producer template used for the DLT
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Kafka producer template.
     *
     * @param producerFactory the producer factory
     * @return the template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Retry with a fixed back-off, then publish the failed record to the DLT.
     *
     * @param kafkaTemplate the producer template for the DLT
     * @return the configured error handler
     */
    @Bean
    public DefaultErrorHandler commonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + dltSuffix, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxAttempts - 1));
    }
}
