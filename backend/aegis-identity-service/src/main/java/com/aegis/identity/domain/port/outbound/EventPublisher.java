package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.event.UserRegistered;

public interface EventPublisher {

    void publish(UserRegistered event);

    void publish(UserAuthenticated event);

    void publish(UserAccountLocked event);
}
