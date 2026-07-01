package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.event.UserRegistered;

public interface EventPublisher {

    void publish(UserRegistered event);
}
