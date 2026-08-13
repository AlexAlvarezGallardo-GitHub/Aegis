package com.aegis.payment.domain.port.outbound;

import com.aegis.payment.domain.event.PaymentExecuted;
import com.aegis.payment.domain.event.PaymentFailed;
import com.aegis.payment.domain.event.PaymentRefunded;
import com.aegis.payment.domain.event.PaymentRequested;
import com.aegis.payment.domain.event.TransferCompleted;
import com.aegis.payment.domain.event.TransferFailed;
import com.aegis.payment.domain.event.TransferRequested;

/**
 * Port for publishing payment domain events to the messaging infrastructure.
 */
public interface EventPublisher {

    /**
     * Publishes a {@link TransferRequested} event.
     *
     * @param event the event to publish
     */
    void publish(TransferRequested event);

    /**
     * Publishes a {@link TransferCompleted} event.
     *
     * @param event the event to publish
     */
    void publish(TransferCompleted event);

    /**
     * Publishes a {@link TransferFailed} event.
     *
     * @param event the event to publish
     */
    void publish(TransferFailed event);

    /**
     * Publishes a {@link PaymentRequested} event.
     *
     * @param event the event to publish
     */
    void publish(PaymentRequested event);

    /**
     * Publishes a {@link PaymentExecuted} event.
     *
     * @param event the event to publish
     */
    void publish(PaymentExecuted event);

    /**
     * Publishes a {@link PaymentFailed} event.
     *
     * @param event the event to publish
     */
    void publish(PaymentFailed event);

    /**
     * Publishes a {@link PaymentRefunded} event.
     *
     * @param event the event to publish
     */
    void publish(PaymentRefunded event);
}
