/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice;

import java.util.Collection;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * WildFly specific extension of {@link TimerService} that allows querying
 * EJB timers by their external identifier.
 */
public interface ExtendedTimerService extends TimerService {

    /**
     * Returns all active timers associated with the specified external identifier.
     *
     * @param externalId the external identifier to search for
     * @return a collection of active timers, or an empty list if none are found
     */
    Collection<Timer> getTimersByExternalId(String externalId);
}