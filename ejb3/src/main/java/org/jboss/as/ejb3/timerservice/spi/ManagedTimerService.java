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

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.io.Serializable;
import java.util.Date;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.ee.Restartable;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Interface for managed {@link jakarta.ejb.TimerService} implementations.
 * @author Paul Ferraro
 */
public interface ManagedTimerService extends TimerService, Restartable {

    /**
     * Returns the managed timer associated with the specified identifier
     * @param id a timer identifier
     * @return a managed timer
     */
    ManagedTimer findTimer(String id);

    /**
     * Returns the invoker for the timed object associated with this timer service.
     * @return a timed object invoker
     */
    TimedObjectInvoker getInvoker();

    @Override
    default Timer createCalendarTimer(ScheduleExpression schedule) {
        return this.createCalendarTimer(schedule, new TimerConfig());
    }

    @Override
    default Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig config) {
        if (initialDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialDuration", Long.toString(initialDuration));
        }
        return this.createIntervalTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, config);
    }

    @Override
    default Timer createSingleActionTimer(long duration, TimerConfig config) {
        if (duration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("duration", Long.toString(duration));
        }
        return this.createSingleActionTimer(new Date(System.currentTimeMillis() + duration), config);
    }

    @Override
    default Timer createTimer(long duration, Serializable info) throws EJBException {
        if (duration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("duration", Long.toString(duration));
        }
        return this.createTimer(new Date(System.currentTimeMillis() + duration), info);
    }

    @Override
    default Timer createTimer(long initialDuration, long intervalDuration, Serializable info) {
        if (initialDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialDuration", Long.toString(initialDuration));
        }
        return this.createTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, info);
    }

    @Override
    default Timer createTimer(Date expiration, Serializable info) {
        return this.createSingleActionTimer(expiration, new TimerConfig(info, true));
    }

    @Override
    default Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info) {
        return this.createIntervalTimer(initialExpiration, intervalDuration, new TimerConfig(info, true));
    }

    /**
     * Validates the invocation context of a given specification method.
     */
    default void validateInvocationContext() {
        AllowedMethodsInformation.checkAllowed(MethodType.TIMER_SERVICE_METHOD);

        if (!this.getInvoker().getComponent().getComponentDescription().isSingleton() && isLifecycleCallbackInvocation()) {
            throw EJB3_TIMER_LOGGER.failToInvokeTimerServiceDoLifecycle();
        }
    }

    /**
     * @return true if the transaction is in a state where synchronizations can be registered
     */
    static Transaction getActiveTransaction() {
        Transaction tx = ContextTransactionManager.getInstance().getTransaction();
        if (tx == null) return null;
        try {
            int status = tx.getStatus();
            switch (status) {
                case Status.STATUS_COMMITTED:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_NO_TRANSACTION:
                case Status.STATUS_ROLLEDBACK:
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_UNKNOWN:
                    return null;
                default:
                    return CurrentSynchronizationCallback.get() != CurrentSynchronizationCallback.CallbackType.BEFORE_COMPLETION ? tx : null;
            }
        } catch (SystemException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Returns true if the {@link CurrentInvocationContext} represents a lifecycle
     * callback invocation. Else returns false.
     * <p>
     * This method internally relies on {@link CurrentInvocationContext#get()} to obtain
     * the current invocation context.
     * <ul>
     * <li>If the context is available then it looks for the method that was invoked.
     * The absence of a method indicates a lifecycle callback.</li>
     * <li>If the context is <i>not</i> available, then this method returns false (i.e.
     * it doesn't consider the current invocation as a lifecycle callback). This is
     * for convenience, to allow the invocation of {@link jakarta.ejb.TimerService} methods
     * in the absence of {@link CurrentInvocationContext}</li>
     * </ul>
     * <p/>
     * </p>
     *
     * @return
     */
    static boolean isLifecycleCallbackInvocation() {
        InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
        // If the method in current invocation context is null,
        // then it represents a lifecycle callback invocation
        return (currentInvocationContext != null) && currentInvocationContext.getMethod() == null;
    }
}
