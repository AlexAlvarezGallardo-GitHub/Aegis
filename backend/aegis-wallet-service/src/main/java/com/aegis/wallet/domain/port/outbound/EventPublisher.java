package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.event.WalletCreated;

public interface EventPublisher {

    void publish(WalletCreated event);
}
