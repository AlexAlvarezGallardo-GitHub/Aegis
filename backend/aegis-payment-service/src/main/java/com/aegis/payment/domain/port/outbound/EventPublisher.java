package com.aegis.payment.domain.port.outbound;

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
}
