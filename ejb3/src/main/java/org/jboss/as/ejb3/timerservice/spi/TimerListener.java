/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

/**
 * Registrar for timers.
 * @author Paul Ferraro
 */
public interface TimerListener {

    /**
     * Registers a timer with the specified identifier.
     * @param id an timer identifier.
     */
    void timerAdded(String id);

    /**
     * Unregisters a timer with the specified identifier.
     * @param id an timer identifier.
     */
    void timerRemoved(String id);
}
