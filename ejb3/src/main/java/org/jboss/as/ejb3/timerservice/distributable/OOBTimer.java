/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.ejb3.timerservice.AbstractManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.common.function.ExceptionConsumer;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Timer implementation for use outside the context of a timeout event.
 * Ensures that all timer methods are invoked within the context of a batch.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class OOBTimer<I> extends AbstractManagedTimer {

    private static final ExceptionFunction<ManagedTimer, TimerHandle, EJBException> GET_HANDLE = ManagedTimer::getHandle;
    private static final ExceptionFunction<ManagedTimer, Serializable, EJBException> GET_INFO = ManagedTimer::getInfo;
    private static final ExceptionFunction<ManagedTimer, Date, EJBException> GET_NEXT_TIMEOUT = ManagedTimer::getNextTimeout;
    private static final ExceptionFunction<ManagedTimer, ScheduleExpression, EJBException> GET_SCHEDULE = ManagedTimer::getSchedule;
    private static final ExceptionFunction<ManagedTimer, Long, EJBException> GET_TIME_REMAINING = ManagedTimer::getTimeRemaining;

    private static final ExceptionFunction<ManagedTimer, Boolean, EJBException> IS_ACTIVE = ManagedTimer::isActive;
    private static final ExceptionFunction<ManagedTimer, Boolean, EJBException> IS_CALENDAR = ManagedTimer::isCalendarTimer;
    private static final ExceptionFunction<ManagedTimer, Boolean, EJBException> IS_CANCELED = ManagedTimer::isCanceled;
    private static final ExceptionFunction<ManagedTimer, Boolean, EJBException> IS_EXPIRED = ManagedTimer::isExpired;
    private static final ExceptionFunction<ManagedTimer, Boolean, EJBException> IS_PERSISTENT = ManagedTimer::isPersistent;

    private static final ExceptionConsumer<ManagedTimer, EJBException> ACTIVATE = ManagedTimer::activate;
    private static final ExceptionConsumer<ManagedTimer, EJBException> CANCEL = ManagedTimer::cancel;
    private static final ExceptionConsumer<ManagedTimer, Exception> INVOKE = ManagedTimer::invoke;
    private static final ExceptionConsumer<ManagedTimer, EJBException> SUSPEND = ManagedTimer::suspend;

    private final TimerManager<I> manager;
    private final I id;
    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;
    private final Function<I, Timer<I>> fixedReader;
    private final Function<I, Timer<I>> dynamicReader;

    public OOBTimer(TimerManager<I> manager, I id, TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
        super(invoker.getTimedObjectId(), id.toString());
        this.manager = manager;
        this.id = id;
        this.invoker = invoker;
        this.synchronizationFactory = synchronizationFactory;
        this.fixedReader = manager::readTimer;
        this.dynamicReader = manager::getTimer;
    }

    @Override
    public void cancel() {
        this.invoke(CANCEL);
    }

    @Override
    public long getTimeRemaining() {
        return this.invokeDynamic(GET_TIME_REMAINING);
    }

    @Override
    public Date getNextTimeout() {
        return this.invokeDynamic(GET_NEXT_TIMEOUT);
    }

    @Override
    public ScheduleExpression getSchedule() {
        return this.invokeFixed(GET_SCHEDULE);
    }

    @Override
    public boolean isPersistent() {
        return this.invokeFixed(IS_PERSISTENT);
    }

    @Override
    public boolean isCalendarTimer() {
        return this.invokeFixed(IS_CALENDAR);
    }

    @Override
    public Serializable getInfo() {
        return this.invokeFixed(GET_INFO);
    }

    @Override
    public TimerHandle getHandle() {
        return this.invokeFixed(GET_HANDLE);
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
        return this.invokeDynamic(IS_ACTIVE);
    }

    @Override
    public boolean isCanceled() {
        return this.invokeDynamic(IS_CANCELED);
    }

    @Override
    public boolean isExpired() {
        return this.invokeDynamic(IS_EXPIRED);
    }

    // Invokes a method reading fixed timer meta data where exclusive access is not required
    private <R, E extends Exception> R invokeFixed(ExceptionFunction<ManagedTimer, R, E> function) throws E {
        return this.invoke(function, this.fixedReader);
    }

    // Invokes a method reading dynamic timer meta data requiring exclusive access
    private <R, E extends Exception> R invokeDynamic(ExceptionFunction<ManagedTimer, R, E> function) throws E {
        return this.invoke(function, this.dynamicReader);
    }

    private <R, E extends Exception> R invoke(ExceptionFunction<ManagedTimer, R, E> function, Function<I, Timer<I>> reader) throws E {
        InterceptorContext interceptorContext = CurrentInvocationContext.get();
        ManagedTimer currentTimer = (interceptorContext != null) ? (ManagedTimer) interceptorContext.getTimer() : null;
        if ((currentTimer != null) && currentTimer.getId().equals(this.id.toString())) {
            return function.apply(currentTimer);
        }
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        @SuppressWarnings("unchecked")
        Map.Entry<Timer<I>, SuspendedBatch> existing = (transaction != null) ? (Map.Entry<Timer<I>, SuspendedBatch>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.id) : null;
        if (existing != null) {
            Timer<I> timer = existing.getKey();
            SuspendedBatch suspendedBatch = existing.getValue();
            return function.apply(new DistributableTimer<>(this.manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory));
        }
        SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
        // Ensure any deferred batch is suspended
        try (Context<Batch> context = suspended.resumeWithContext()) {
            try (Batch batch = context.get()) {
                Timer<I> timer = reader.apply(this.id);
                if (timer == null) {
                    throw EjbLogger.ROOT_LOGGER.timerWasCanceled(this.id.toString());
                }
                try (Context<SuspendedBatch> suspendedContext = batch.suspendWithContext()) {
                    return function.apply(new DistributableTimer<>(this.manager, timer, suspendedContext.get(), this.invoker, this.synchronizationFactory));
                }
            }
        }
    }

    private <E extends Exception> void invoke(ExceptionConsumer<ManagedTimer, E> consumer) throws E {
        this.invoke(new ExceptionFunction<ManagedTimer, Void, E>() {
            @Override
            public Void apply(ManagedTimer timer) throws E {
                consumer.accept(timer);
                return null;
            }
        }, this.dynamicReader);
    }
}
