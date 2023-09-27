/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import jakarta.ejb.Timer;

import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * Interface for managed {@link jakarta.ejb.Timer} implementations.
 * @author Paul Ferraro
 */
public interface ManagedTimer extends Timer {

    /**
     * The unique identifier of this timer.
     * @return a unique identifier
     */
    String getId();

    /**
     * Indicates whether this timer is active, i.e. not suspended.
     * @return true, if this timer is active, false otherwise.
     */
    boolean isActive();

    /**
     * Indicates whether this timer was canceled, i.e. via {@link Timer#cancel()}.
     * @return true, if this timer was canceled, false otherwise.
     */
    boolean isCanceled();

    /**
     * Indicates whether this timer has expired, i.e. it has no more timeouts.
     * An interval timer will always return false.
     * @return true, if this timer has expired, false otherwise.
     */
    boolean isExpired();

    /**
     * Activates a previously suspended timer.  Once active, the timer will receive timeout events as usual, including any timeouts missed while inactive.
     */
    void activate();

    /**
     * Suspends a previously active timer. While suspended, the timer will not receive timeout events.
     */
    void suspend();

    /**
     * Invokes the timeout method associated with this timer.  Has no impact on this timer's schedule.
     * @throws Exception
     */
    void invoke() throws Exception;

    /**
     * Validates the invocation context of a given specification method.
     */
    default void validateInvocationContext() {
        if (this.isCanceled()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.timerWasCanceled(this.getId());
        }
        if (this.isExpired()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.timerHasExpired(this.getId());
        }
        AllowedMethodsInformation.checkAllowed(MethodType.TIMER_SERVICE_METHOD);
    }
}
