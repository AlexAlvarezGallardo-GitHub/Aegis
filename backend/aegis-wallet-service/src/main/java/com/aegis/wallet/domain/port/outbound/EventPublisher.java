package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.event.WalletCreated;

/**
 * Port for publishing domain events to the messaging infrastructure.
 */
public interface EventPublisher {

    void publish(WalletCreated event);
}
