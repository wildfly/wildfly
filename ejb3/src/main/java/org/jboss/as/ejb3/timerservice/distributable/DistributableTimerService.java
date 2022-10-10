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

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerConfig;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.spi.AutoTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;

/**
 * EJB specification facade for a distributable EJB timer manager.
 * @author Paul Ferraro
 */
public class DistributableTimerService<I> implements ManagedTimerService, Function<I, jakarta.ejb.Timer> {

    private final TimerServiceRegistry registry;
    private final TimedObjectInvoker invoker;
    private final TimerManager<I, Batch> manager;
    private final TimerSynchronizationFactory<I> synchronizationFactory;
    private final Function<String, I> identifierParser;
    private final Predicate<TimerConfig> filter;

    public DistributableTimerService(DistributableTimerServiceConfiguration<I> configuration, TimerManager<I, Batch> manager) {
        this.invoker = configuration.getInvoker();
        this.identifierParser = configuration.getIdentifierParser();
        this.filter = configuration.getTimerFilter();
        this.registry = configuration.getTimerServiceRegistry();
        this.manager = manager;
        this.synchronizationFactory = configuration.getTimerSynchronizationFactory();
    }

    @Override
    public TimedObjectInvoker getInvoker() {
        return this.invoker;
    }

    @Override
    public void start() {
        this.manager.start();

        // Create and start auto-timers, if they do not already exist
        Supplier<I> identifierFactory = this.manager.getIdentifierFactory();
        EJBComponent component = this.invoker.getComponent();
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            for (Map.Entry<Method, List<AutoTimer>> entry : component.getComponentDescription().getScheduleMethods().entrySet()) {
                Method method = entry.getKey();
                ListIterator<AutoTimer> timers = entry.getValue().listIterator();
                while (timers.hasNext()) {
                    AutoTimer autoTimer = timers.next();
                    if (this.filter.test(autoTimer.getTimerConfig())) {
                        Timer<I> timer = this.manager.createTimer(identifierFactory.get(), new SimpleScheduleTimerConfiguration(autoTimer.getScheduleExpression()), autoTimer.getTimerConfig().getInfo(), method, timers.previousIndex());
                        if (timer != null) {
                            timer.activate();
                        }
                    }
                }
            }
        }

        this.registry.registerTimerService(this);
    }

    @Override
    public void stop() {
        this.registry.unregisterTimerService(this);
        this.manager.stop();
    }

    @Override
    public ManagedTimer findTimer(String timerId) {
        InterceptorContext interceptorContext = CurrentInvocationContext.get();
        ManagedTimer currentTimer = (interceptorContext != null) ? (ManagedTimer) interceptorContext.getTimer() : null;
        if ((currentTimer != null) && currentTimer.getId().equals(timerId)) {
            return currentTimer;
        }
        try {
            I id = this.identifierParser.apply(timerId);
            try (Batch batch = this.manager.getBatcher().createBatch()) {
                Timer<I> timer = this.manager.getTimer(id);
                return (timer != null) ? new OOBTimer<>(this.manager, timer.getId(), this.invoker, this.synchronizationFactory) : null;
            }
        } catch (IllegalArgumentException e) {
            // We couldn't parse the timerId
            return null;
        }
    }

    @Override
    public jakarta.ejb.Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig config) {
        return this.createEJBTimer(new ScheduleTimerFactory<>(schedule, config.getInfo()));
    }

    @Override
    public jakarta.ejb.Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig config) {
        return this.createEJBTimer(new IntervalTimerFactory<>(initialExpiration.toInstant(), Duration.ofMillis(intervalDuration), config.getInfo()));
    }

    @Override
    public jakarta.ejb.Timer createSingleActionTimer(Date expiration, TimerConfig config) {
        return this.createEJBTimer(new IntervalTimerFactory<>(expiration.toInstant(), null, config.getInfo()));
    }

    private jakarta.ejb.Timer createEJBTimer(BiFunction<TimerManager<I, Batch>, I, Timer<I>> factory) {
        Timer<I> timer = this.createTimer(factory);
        return new OOBTimer<>(this.manager, timer.getId(), this.invoker, this.synchronizationFactory);
    }

    private Timer<I> createTimer(BiFunction<TimerManager<I, Batch>, I, Timer<I>> factory) {
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        boolean close = true;
        Batcher<Batch> batcher = this.manager.getBatcher();
        Batch batch = batcher.createBatch();
        try {
            I id = this.manager.getIdentifierFactory().get();
            Timer<I> timer = factory.apply(this.manager, id);
            if (timer.getMetaData().getNextTimeout() != null) {
                if (transaction != null) {
                    // Transactional case: Activate timer on tx commit
                    // Cancel timer on tx rollback
                    Batch suspendedBatch = batcher.suspendBatch();
                    transaction.registerSynchronization(this.synchronizationFactory.createActivateSynchronization(timer, suspendedBatch, batcher));
                    TransactionSynchronizationRegistry tsr = this.invoker.getComponent().getTransactionSynchronizationRegistry();
                    // Store suspended batch in TSR so we can resume it later, if necessary
                    tsr.putResource(id, new SimpleImmutableEntry<>(timer, suspendedBatch));
                    @SuppressWarnings("unchecked")
                    Set<I> inactiveTimers = (Set<I>) tsr.getResource(this.manager);
                    if (inactiveTimers == null) {
                        inactiveTimers = new TreeSet<>();
                        tsr.putResource(this.manager, inactiveTimers);
                    }
                    inactiveTimers.add(id);
                    close = false;
                } else {
                    // Non-transactional case: activate timer immediately.
                    this.synchronizationFactory.getActivateTask().accept(timer);
                }
            } else {
                // This timer will never expire!
                timer.cancel();
            }
            return timer;
        } catch (RollbackException e) {
            // getActiveTransaction() would have returned null
            throw new IllegalStateException(e);
        } catch (SystemException e) {
            batch.discard();
            throw new EJBException(e);
        } finally {
            if (close) {
                batch.close();
            }
        }
    }

    @Override
    public Collection<jakarta.ejb.Timer> getTimers() throws EJBException {
        this.validateInvocationContext();

        @SuppressWarnings("unchecked")
        Set<I> inactiveTimers = (ManagedTimerService.getActiveTransaction() != null) ? (Set<I>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.manager) : null;
        try (Stream<I> activeTimers = this.manager.getActiveTimers()) {
            Stream<I> timers = (inactiveTimers != null) ? Stream.concat(activeTimers, inactiveTimers.stream()) : activeTimers;
            return Collections.unmodifiableCollection(timers.map(this).collect(Collectors.toList()));
        }
    }

    @Override
    public jakarta.ejb.Timer apply(I id) {
        return new OOBTimer<>(this.manager, id, this.invoker, this.synchronizationFactory);
    }

    @Override
    public Collection<jakarta.ejb.Timer> getAllTimers() throws EJBException {
        this.validateInvocationContext();
        return this.registry.getAllTimers();
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.invoker.getTimedObjectId());
    }

    static class IntervalTimerFactory<I> implements BiFunction<TimerManager<I, Batch>, I, Timer<I>>, IntervalTimerConfiguration {
        private final Instant start;
        private final Duration interval;
        private final Object info;

        IntervalTimerFactory(Instant start, Duration interval, Object info) {
            this.start = start;
            this.interval = interval;
            this.info = info;
        }

        @Override
        public Instant getStart() {
            return this.start;
        }

        @Override
        public Duration getInterval() {
            return this.interval;
        }

        @Override
        public Timer<I> apply(TimerManager<I, Batch> manager, I id) {
            return manager.createTimer(id, this, this.info);
        }
    }

    static class SimpleScheduleTimerConfiguration implements ScheduleTimerConfiguration {
        private final ImmutableScheduleExpression expression;

        SimpleScheduleTimerConfiguration(ScheduleExpression expression) {
            this.expression = new SimpleImmutableScheduleExpression(expression);
        }

        @Override
        public ImmutableScheduleExpression getScheduleExpression() {
            return this.expression;
        }
    }

    static class ScheduleTimerFactory<I> implements BiFunction<TimerManager<I, Batch>, I, Timer<I>> {
        private final ScheduleTimerConfiguration configuration;
        private final Object info;

        ScheduleTimerFactory(ScheduleExpression expression, Object info) {
            this.configuration = new SimpleScheduleTimerConfiguration(expression);
            this.info = info;
        }

        @Override
        public Timer<I> apply(TimerManager<I, Batch> manager, I id) {
            return manager.createTimer(id, this.configuration, this.info);
        }
    }
}
