package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.event.UserRegistered;

/**
 * Outbound port for publishing identity domain events to the messaging infrastructure.
 */
public interface EventPublisher {

    /**
     * Publishes a {@link UserRegistered} event.
     *
     * @param event the user registered domain event
     */
    void publish(UserRegistered event);

    /**
     * Publishes a {@link UserAuthenticated} event.
     *
     * @param event the user authenticated domain event
     */
    void publish(UserAuthenticated event);

    /**
     * Publishes a {@link UserAccountLocked} event.
     *
     * @param event the user account locked domain event
     */
    void publish(UserAccountLocked event);
}
