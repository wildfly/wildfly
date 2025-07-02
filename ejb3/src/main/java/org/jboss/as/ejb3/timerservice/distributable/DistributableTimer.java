/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.ejb.EJBException;
import jakarta.ejb.NoMoreTimeoutsException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerHandle;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.TimerHandleImpl;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;

/**
 * Managed timer facade for a distributable EJB timer.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class DistributableTimer<I> implements ManagedTimer {

    private final TimerManager<I> manager;
    private final Timer<I> timer;
    private final SuspendedBatch suspendedBatch;
    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;

    public DistributableTimer(TimerManager<I> manager, Timer<I> timer, SuspendedBatch suspendedBatch, TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
        this.manager = manager;
        this.timer = timer;
        this.suspendedBatch = suspendedBatch;
        this.invoker = invoker;
        this.synchronizationFactory = synchronizationFactory;
    }

    @Override
    public String getId() {
        return this.timer.getId().toString();
    }

    @Override
    public boolean isActive() {
        return this.timer.isActive() && !this.timer.isCanceled();
    }

    @Override
    public boolean isCanceled() {
        return this.timer.isCanceled();
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void activate() {
        this.timer.activate();
    }

    @Override
    public void suspend() {
        this.timer.suspend();
    }

    @Override
    public void invoke() throws Exception {
        Predicate<Method> matcher = this.timer.getMetaData().getTimeoutMatcher();
        EJBComponent component = this.invoker.getComponent();
        EJBComponentDescription description = component.getComponentDescription();
        Method method = description.getScheduleMethods().keySet().stream().filter(matcher).findFirst().orElse(component.getTimeoutMethod());
        this.invoker.callTimeout(this, method);
    }

    @Override
    public void cancel() {
        this.validateInvocationContext();
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            if (transaction != null) {
                // Cancel timer on tx commit
                // Reactivate timer on tx rollback
                this.timer.suspend();
                try {
                    transaction.registerSynchronization(this.synchronizationFactory.createCancelSynchronization(this.timer, this.manager.getBatchFactory(), this.suspendedBatch));
                } catch (RollbackException e) {
                    // getActiveTransaction() would have returned null
                    throw new IllegalStateException(e);
                } catch (SystemException e) {
                    context.get().discard();
                    throw new EJBException(e);
                }
                @SuppressWarnings("unchecked")
                Set<I> inactiveTimers = (Set<I>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.manager);
                if (inactiveTimers != null) {
                    inactiveTimers.remove(this.timer.getId());
                }
            } else {
                this.synchronizationFactory.getCancelTask().accept(this.timer);
            }
        }
    }

    @Override
    public long getTimeRemaining() {
        this.validateInvocationContext();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            Instant next = this.timer.getMetaData().getNextTimeout().orElseThrow(NoMoreTimeoutsException::new);
            return Duration.between(Instant.now(), next).toMillis();
        }
    }

    @Override
    public Date getNextTimeout() {
        this.validateInvocationContext();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            Instant next = this.timer.getMetaData().getNextTimeout().orElseThrow(NoMoreTimeoutsException::new);
            return Date.from(next);
        }
    }

    @Override
    public Serializable getInfo() {
        this.validateInvocationContext();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            return (Serializable) this.timer.getMetaData().getContext();
        }
    }

    @Override
    public TimerHandle getHandle() {
        this.validateInvocationContext();
        // for non-persistent timers throws an exception (mandated by EJB3 spec)
        if (!this.timer.getMetaData().isPersistent()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidTimerHandlersForPersistentTimers("EJB3.1 Spec 18.2.6");
        }
        // Create handle w/OOB timer
        return new TimerHandleImpl(new OOBTimer<>(this.manager, this.timer.getId(), this.invoker, this.synchronizationFactory), this.invoker.getComponent());
    }

    @Override
    public ScheduleExpression getSchedule() {
        this.validateInvocationContext();
        if (!this.timer.getMetaData().getType().isCalendar()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidTimerNotCalendarBaseTimer(this);
        }
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            ImmutableScheduleExpression expression = this.timer.getMetaData().getConfiguration(ScheduleTimerConfiguration.class).getScheduleExpression();
            ScheduleExpression result = new ScheduleExpression()
                    .second(expression.getSecond())
                    .minute(expression.getMinute())
                    .hour(expression.getHour())
                    .dayOfMonth(expression.getDayOfMonth())
                    .month(expression.getMonth())
                    .dayOfWeek(expression.getDayOfWeek())
                    .year(expression.getYear());
            Instant start = expression.getStart();
            if (start != null) {
                result.start(Date.from(start));
            }
            Instant end = expression.getEnd();
            if (end != null) {
                result.end(Date.from(end));
            }
            ZoneId zone = expression.getZone();
            if (zone != SimpleImmutableScheduleExpression.DEFAULT_ZONE_ID) {
                result.timezone(zone.getId());
            }
            return result;
        }
    }

    @Override
    public boolean isCalendarTimer() {
        this.validateInvocationContext();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            return this.timer.getMetaData().getType().isCalendar();
        }
    }

    @Override
    public boolean isPersistent() {
        this.validateInvocationContext();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            return this.timer.getMetaData().isPersistent();
        }
    }

    @Override
    public int hashCode() {
        return this.timer.hashCode();
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
