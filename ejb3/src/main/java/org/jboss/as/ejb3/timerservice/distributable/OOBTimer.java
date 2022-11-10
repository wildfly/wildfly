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

package org.jboss.as.ejb3.timerservice.distributable;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerHandle;
import jakarta.transaction.Transaction;

import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.common.function.ExceptionConsumer;

/**
 * Timer implementation for use outside the context of a timeout event.
 * Ensures that all timer methods are invoked within the context of a batch.
 * @author Paul Ferraro
 */
public class OOBTimer<I> implements ManagedTimer {

    private static final Function<ManagedTimer, TimerHandle> GET_HANDLE = ManagedTimer::getHandle;
    private static final Function<ManagedTimer, Serializable> GET_INFO = ManagedTimer::getInfo;
    private static final Function<ManagedTimer, Date> GET_NEXT_TIMEOUT = ManagedTimer::getNextTimeout;
    private static final Function<ManagedTimer, ScheduleExpression> GET_SCHEDULE = ManagedTimer::getSchedule;
    private static final Function<ManagedTimer, Long> GET_TIME_REMAINING = ManagedTimer::getTimeRemaining;

    private static final Function<ManagedTimer, Boolean> IS_ACTIVE = ManagedTimer::isActive;
    private static final Function<ManagedTimer, Boolean> IS_CALENDAR = ManagedTimer::isCalendarTimer;
    private static final Function<ManagedTimer, Boolean> IS_CANCELED = ManagedTimer::isCanceled;
    private static final Function<ManagedTimer, Boolean> IS_EXPIRED = ManagedTimer::isExpired;
    private static final Function<ManagedTimer, Boolean> IS_PERSISTENT = ManagedTimer::isPersistent;

    private static final ExceptionConsumer<ManagedTimer, EJBException> ACTIVATE = ManagedTimer::activate;
    private static final ExceptionConsumer<ManagedTimer, EJBException> CANCEL = ManagedTimer::cancel;
    private static final ExceptionConsumer<ManagedTimer, Exception> INVOKE = ManagedTimer::invoke;
    private static final ExceptionConsumer<ManagedTimer, EJBException> SUSPEND = ManagedTimer::suspend;

    private final TimerManager<I, Batch> manager;
    private final I id;
    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;

    public OOBTimer(TimerManager<I, Batch> manager, I id, TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
        this.manager = manager;
        this.id = id;
        this.invoker = invoker;
        this.synchronizationFactory = synchronizationFactory;
    }

    @Override
    public void cancel() {
        this.invoke(CANCEL);
    }

    @Override
    public long getTimeRemaining() {
        return this.invoke(GET_TIME_REMAINING);
    }

    @Override
    public Date getNextTimeout() {
        return this.invoke(GET_NEXT_TIMEOUT);
    }

    @Override
    public ScheduleExpression getSchedule() {
        return this.invoke(GET_SCHEDULE);
    }

    @Override
    public boolean isPersistent() {
        return this.invoke(IS_PERSISTENT);
    }

    @Override
    public boolean isCalendarTimer() {
        return this.invoke(IS_CALENDAR);
    }

    @Override
    public Serializable getInfo() {
        return this.invoke(GET_INFO);
    }

    @Override
    public TimerHandle getHandle() {
        return this.invoke(GET_HANDLE);
    }

    @Override
    public String getId() {
        return this.id.toString();
    }

    @Override
    public void activate() {
        this.invoke(ACTIVATE);
    }

    @Override
    public void suspend() {
        this.invoke(SUSPEND);
    }

    @Override
    public void invoke() throws Exception {
        this.invoke(INVOKE);
    }

    @Override
    public boolean isActive() {
        return this.invoke(IS_ACTIVE);
    }

    @Override
    public boolean isCanceled() {
        return this.invoke(IS_CANCELED);
    }

    @Override
    public boolean isExpired() {
        return this.invoke(IS_EXPIRED);
    }

    private <R> R invoke(Function<ManagedTimer, R> function) {
        InterceptorContext interceptorContext = CurrentInvocationContext.get();
        ManagedTimer currentTimer = (interceptorContext != null) ? (ManagedTimer) interceptorContext.getTimer() : null;
        if ((currentTimer != null) && currentTimer.getId().equals(this.id.toString())) {
            return function.apply(currentTimer);
        }
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        @SuppressWarnings("unchecked")
        Map.Entry<Timer<I>, Batch> existing = (transaction != null) ? (Map.Entry<Timer<I>, Batch>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.id) : null;
        if (existing != null) {
            Timer<I> timer = existing.getKey();
            Batch suspendedBatch = existing.getValue();
            return function.apply(new DistributableTimer<>(this.manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory));
        }
        Batcher<Batch> batcher = this.manager.getBatcher();
        try (Batch batch = batcher.createBatch()) {
            Timer<I> timer = this.manager.getTimer(this.id);
            if (timer == null) {
                throw EjbLogger.ROOT_LOGGER.timerWasCanceled(this.id.toString());
            }
            Batch suspendedBatch = batcher.suspendBatch();
            try {
                return function.apply(new DistributableTimer<>(this.manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory));
            } finally {
                batcher.resumeBatch(suspendedBatch);
            }
        }
    }

    private <E extends Exception> void invoke(ExceptionConsumer<ManagedTimer, E> consumer) throws E {
        InterceptorContext interceptorContext = CurrentInvocationContext.get();
        ManagedTimer currentTimer = (interceptorContext != null) ? (ManagedTimer) interceptorContext.getTimer() : null;
        if ((currentTimer != null) && currentTimer.getId().equals(this.id.toString())) {
            consumer.accept(currentTimer);
        } else {
            Transaction transaction = ManagedTimerService.getActiveTransaction();
            @SuppressWarnings("unchecked")
            Map.Entry<Timer<I>, Batch> existing = (transaction != null) ? (Map.Entry<Timer<I>, Batch>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.id) : null;
            if (existing != null) {
                Timer<I> timer = existing.getKey();
                Batch suspendedBatch = existing.getValue();
                consumer.accept(new DistributableTimer<>(this.manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory));
            } else {
                Batcher<Batch> batcher = this.manager.getBatcher();
                try (Batch batch = batcher.createBatch()) {
                    Timer<I> timer = this.manager.getTimer(this.id);
                    if (timer == null) {
                        throw EjbLogger.ROOT_LOGGER.timerWasCanceled(this.id.toString());
                    }
                    Batch suspendedBatch = batcher.suspendBatch();
                    try {
                        consumer.accept(new DistributableTimer<>(this.manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory));
                    } finally {
                        batcher.resumeBatch(suspendedBatch);
                    }
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ManagedTimer)) return false;
        return this.getId().equals(((ManagedTimer) object).getId());
    }

    @Override
    public String toString() {
        return this.getId();
    }
}
