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

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.TimerHandleImpl;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;

/**
 * Managed timer facade for a distributable EJB timer.
 * @author Paul Ferraro
 */
public class DistributableTimer<I> implements ManagedTimer {

    private final TimerManager<I, Batch> manager;
    private final Timer<I> timer;
    private final Batch suspendedBatch;
    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;

    public DistributableTimer(TimerManager<I, Batch> manager, Timer<I> timer, Batch suspendedBatch, TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
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
        if (matcher != null) {
            this.invoker.callTimeout(this, this.invoker.getComponent().getComponentDescription().getScheduleMethods().keySet().stream().filter(matcher).findFirst().get());
        } else {
            this.invoker.callTimeout(this);
        }
    }

    @Override
    public void cancel() {
        this.validateInvocationContext();
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
            if (transaction != null) {
                // Cancel timer on tx commit
                // Reactivate timer on tx rollback
                this.timer.suspend();
                transaction.registerSynchronization(this.synchronizationFactory.createCancelSynchronization(this.timer, this.suspendedBatch, this.manager.getBatcher()));
                @SuppressWarnings("unchecked")
                Set<I> inactiveTimers = (Set<I>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.manager);
                if (inactiveTimers != null) {
                    inactiveTimers.remove(this.timer.getId());
                }
            } else {
                this.synchronizationFactory.getCancelTask().accept(this.timer);
            }
        } catch (RollbackException e) {
            // getActiveTransaction() would have returned null
            throw new IllegalStateException(e);
        } catch (SystemException e) {
            this.suspendedBatch.discard();
            throw new EJBException(e);
        }
    }

    @Override
    public long getTimeRemaining() {
        this.validateInvocationContext();
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
            Instant next = this.timer.getMetaData().getNextTimeout();
            if (next == null) {
                throw new NoMoreTimeoutsException();
            }
            return Duration.between(Instant.now(), next).toMillis();
        }
    }

    @Override
    public Date getNextTimeout() {
        this.validateInvocationContext();
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
            Instant next = this.timer.getMetaData().getNextTimeout();
            if (next == null) {
                throw new NoMoreTimeoutsException();
            }
            return Date.from(next);
        }
    }

    @Override
    public Serializable getInfo() {
        this.validateInvocationContext();
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
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
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
            // Create handle w/OOB timer
            return new TimerHandleImpl(new OOBTimer<>(this.manager, this.timer.getId(), this.invoker, this.synchronizationFactory), this.invoker.getComponent());
        }
    }

    @Override
    public ScheduleExpression getSchedule() {
        this.validateInvocationContext();
        if (!this.timer.getMetaData().getType().isCalendar()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidTimerNotCalendarBaseTimer(this);
        }
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
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
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
            return this.timer.getMetaData().getType().isCalendar();
        }
    }

    @Override
    public boolean isPersistent() {
        this.validateInvocationContext();
        try (BatchContext context = this.manager.getBatcher().resumeBatch(this.suspendedBatch)) {
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
