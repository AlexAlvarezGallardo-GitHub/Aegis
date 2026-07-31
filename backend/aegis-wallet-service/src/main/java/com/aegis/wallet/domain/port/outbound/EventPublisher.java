package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.event.FundsDeposited;
import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.event.WalletCreated;

public interface EventPublisher {

    void publish(WalletCreated event);

    void publish(WalletBalanceAdjusted event);

    void publish(FundsDeposited event);
}
