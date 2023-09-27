/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import java.util.Collection;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * Registry of timer services for a given deployment, used to implement {@link TimerService#getAllTimers()}.
 * @author Paul Ferraro
 */
public interface TimerServiceRegistry {

    /**
     * Registers the specified timer service.
     * @param service a timer service
     */
    void registerTimerService(TimerService service);

    /**
     * Unregisters the specified timer service.
     * @param service a timer service
     */
    void unregisterTimerService(TimerService service);

    /**
     * Returns the timers for all registered timer services.
     * @return a collection of timers
     */
    Collection<Timer> getAllTimers();
}
