/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import java.util.Collection;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import org.jboss.as.ejb3.timerservice.ExtendedTimerService;

/**
 * Registry of timer services for a given deployment, used to implement {@link TimerService#getAllTimers()}.
 * @author Paul Ferraro
 */
public interface TimerServiceRegistry {

    /**
     * Registers the specified timer service.
     * @param service a timer service
     */
    void registerTimerService(ExtendedTimerService service);

    /**
     * Unregisters the specified timer service.
     * @param service a timer service
     */
    void unregisterTimerService(ExtendedTimerService service);

    /**
     * Returns the timers for all registered timer services.
     * @return a collection of timers
     */
    Collection<Timer> getAllTimers();

    public Collection<Timer> getTimersByExternalId(String externalId);
}
