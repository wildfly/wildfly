/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.spi.AutoTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.server.service.DecoratedService;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerConfig;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * EJB specification facade for a distributable EJB timer manager.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class DistributableTimerService<I> extends DecoratedService implements ManagedTimerService {

    private final TimerServiceRegistry registry;
    private final TimedObjectInvoker invoker;
    private final TimerManager<I> manager;
    private final TimerSynchronizationFactory<I> synchronizationFactory;
    private final Function<String, I> identifierParser;
    private final Predicate<TimerConfig> filter;

    public DistributableTimerService(DistributableTimerServiceConfiguration<I> configuration, TimerManager<I> manager) {
        super(manager);
        this.invoker = configuration.getInvoker();
        this.identifierParser = configuration.getIdentifierParser();
        this.filter = configuration.getTimerFilter();
        this.registry = configuration.getTimerServiceRegistry();
        this.manager = manager;
        this.synchronizationFactory = configuration.getTimerSynchronizationFactory();

        // Create auto-timers if they do not already exist
        Supplier<I> identifierFactory = this.manager.getIdentifierFactory();
        EJBComponent component = this.invoker.getComponent();
        try (Batch batch = this.manager.getBatchFactory().get()) {
            for (Map.Entry<Method, List<AutoTimer>> entry : component.getComponentDescription().getScheduleMethods().entrySet()) {
                Method method = entry.getKey();
                ListIterator<AutoTimer> timers = entry.getValue().listIterator();
                while (timers.hasNext()) {
                    AutoTimer autoTimer = timers.next();
                    if (this.filter.test(autoTimer.getTimerConfig())) {
                        // Create, but do not activate auto-timers (the manager is not yet started)
                        // These will auto-activate during TimerManager.start()
                        this.manager.createTimer(identifierFactory.get(), new SimpleScheduleTimerConfiguration(autoTimer.getScheduleExpression()), autoTimer.getTimerConfig().getInfo(), method, timers.previousIndex());
                    }
                }
            }
        }
        this.registry.registerTimerService(this);
    }

    @Override
    public TimedObjectInvoker getInvoker() {
        return this.invoker;
    }

    @Override
    public void close() {
        this.manager.close();
        this.registry.unregisterTimerService(this);
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
            try (Batch batch = this.manager.getBatchFactory().get()) {
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

    private jakarta.ejb.Timer createEJBTimer(BiFunction<TimerManager<I>, I, Timer<I>> factory) {
        Timer<I> timer = this.createTimer(factory);
        return new OOBTimer<>(this.manager, timer.getId(), this.invoker, this.synchronizationFactory);
    }

    private Timer<I> createTimer(BiFunction<TimerManager<I>, I, Timer<I>> factory) {
        Transaction transaction = ManagedTimerService.getActiveTransaction();
        SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
        try (Context<Batch> context = suspended.resumeWithContext()) {
            try (Batch batch = context.get()) {
                I id = this.manager.getIdentifierFactory().get();
                Timer<I> timer = factory.apply(this.manager, id);
                if (timer.getMetaData().getNextTimeout() != null) {
                    if (transaction != null) {
                        // Transactional case: Activate timer on tx commit
                        // Cancel timer on tx rollback
                        this.registerSynchronization(transaction, timer);
                        TransactionSynchronizationRegistry tsr = this.invoker.getComponent().getTransactionSynchronizationRegistry();
                        // Store suspended batch in TSR so we can resume it later, if necessary
                        tsr.putResource(id, new SimpleImmutableEntry<>(timer, suspended));
                        @SuppressWarnings("unchecked")
                        Set<I> inactiveTimers = (Set<I>) tsr.getResource(this.manager);
                        if (inactiveTimers == null) {
                            inactiveTimers = new TreeSet<>();
                            tsr.putResource(this.manager, inactiveTimers);
                        }
                        inactiveTimers.add(id);
                    } else {
                        // Non-transactional case: activate timer immediately.
                        this.synchronizationFactory.getActivateTask().accept(timer);
                    }
                } else {
                    // This timer will never expire!
                    timer.cancel();
                }
                return timer;
            }
        }
    }

    private void registerSynchronization(Transaction transaction, Timer<I> timer) {
        @SuppressWarnings("resource") // Closed via synchronization
        Batch batch = this.manager.getBatchFactory().get();
        try (Context<SuspendedBatch> context = batch.suspendWithContext()) {
            transaction.registerSynchronization(this.synchronizationFactory.createActivateSynchronization(timer, context.get()));
        } catch (RollbackException | SystemException e) {
            batch.close();
            throw new EJBException(e);
        }
    }

    @Override
    public Collection<jakarta.ejb.Timer> getTimers() throws EJBException {
        this.validateInvocationContext();

        Collection<jakarta.ejb.Timer> timers = new LinkedList<>();
        @SuppressWarnings("unchecked")
        Set<I> inactiveTimers = (ManagedTimerService.getActiveTransaction() != null) ? (Set<I>) this.invoker.getComponent().getTransactionSynchronizationRegistry().getResource(this.manager) : null;
        if (inactiveTimers != null) {
            this.addTimers(timers, inactiveTimers);
        }
        try (Stream<I> activeTimers = this.manager.getActiveTimers()) {
            this.addTimers(timers, activeTimers::iterator);
        }
        return Collections.unmodifiableCollection(timers);
    }

    private void addTimers(Collection<jakarta.ejb.Timer> timers, Iterable<I> timerIds) {
        ManagedTimer currentTimer = Optional.ofNullable(CurrentInvocationContext.get()).map(InterceptorContext::getTimer).filter(ManagedTimer.class::isInstance).map(ManagedTimer.class::cast).orElse(null);
        for (I timerId : timerIds) {
            ManagedTimer timer = new OOBTimer<>(this.manager, timerId, this.invoker, this.synchronizationFactory);
            // Use timer from interceptor context, if one exists
            timers.add(Objects.equals(timer, currentTimer) ? currentTimer : timer);
        }
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

    static class IntervalTimerFactory<I> implements BiFunction<TimerManager<I>, I, Timer<I>>, IntervalTimerConfiguration {
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
        public Timer<I> apply(TimerManager<I> manager, I id) {
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

    static class ScheduleTimerFactory<I> implements BiFunction<TimerManager<I>, I, Timer<I>> {
        private final ScheduleTimerConfiguration configuration;
        private final Object info;

        ScheduleTimerFactory(ScheduleExpression expression, Object info) {
            this.configuration = new SimpleScheduleTimerConfiguration(expression);
            this.info = info;
        }

        @Override
        public Timer<I> apply(TimerManager<I> manager, I id) {
            return manager.createTimer(id, this.configuration, this.info);
        }
    }
}
