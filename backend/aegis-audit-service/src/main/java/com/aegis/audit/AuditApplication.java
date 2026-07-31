package com.aegis.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Aegis Audit Service.
 * <p>
 * This service consumes financial events from Kafka and persists audit records
 * for regulatory compliance and traceability.
 * </p>
 */
@SpringBootApplication
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
