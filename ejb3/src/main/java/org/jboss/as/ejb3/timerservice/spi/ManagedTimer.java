/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
