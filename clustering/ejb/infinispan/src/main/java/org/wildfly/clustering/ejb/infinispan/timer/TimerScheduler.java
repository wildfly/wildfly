/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.server.infinispan.scheduler.AbstractCacheEntryScheduler;
import org.wildfly.clustering.server.local.scheduler.LocalScheduler;
import org.wildfly.clustering.server.local.scheduler.LocalSchedulerConfiguration;
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A scheduler of timer timeouts.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <V> the timer metadata value type
 */
public class TimerScheduler<I, V> extends AbstractCacheEntryScheduler<I, TimerMetaDataKey<I>, V, TimeoutMetaData> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(TimerScheduler.class, WildFlySecurityManager.getClassLoaderPrivileged(TimerScheduler.class));

    private final TimerFactory<I, V> factory;

    public TimerScheduler(String name, TimerFactory<I, V> factory, TimerManager<I> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry) {
        this(name, factory, manager, locality, closeTimeout, registry, ScheduledEntries.sorted(), Executors.newSingleThreadExecutor(THREAD_FACTORY));
    }

    private TimerScheduler(String name, TimerFactory<I, V> factory, TimerManager<I> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry, ScheduledEntries<I, Instant> entries, ExecutorService executor) {
        this(name, entries, new InvokeTask<>(factory, manager, locality, entries, registry, executor), closeTimeout, registry, executor, factory);
    }

    private <T extends Predicate<I> & Consumer<Scheduler<I, TimeoutMetaData>>> TimerScheduler(String name, ScheduledEntries<I, Instant> entries, T invokeTask, Duration closeTimeout, TimerRegistry<I> registry, ExecutorService executor, TimerFactory<I, V> factory) {
        this(new LocalSchedulerConfiguration<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public ScheduledEntries<I, Instant> getScheduledEntries() {
                return entries;
            }

            @Override
            public Predicate<I> getTask() {
                return invokeTask;
            }

            @Override
            public ThreadFactory getThreadFactory() {
                return THREAD_FACTORY;
            }

            @Override
            public Duration getCloseTimeout() {
                return closeTimeout;
            }
        }, registry, executor, invokeTask, factory);
    }

    private TimerScheduler(LocalSchedulerConfiguration<I> schedulerConfig, TimerRegistry<I> registry, ExecutorService executor, Consumer<Scheduler<I, TimeoutMetaData>> injector, TimerFactory<I, V> factory) {
        this(new LocalScheduler<>(schedulerConfig) {
            @Override
            public void schedule(I id, Instant instant) {
                super.schedule(id, instant);
                registry.register(id);
            }

            @Override
            public void cancel(I id) {
                registry.unregister(id);
                super.cancel(id);
            }

            @Override
            public void close() {
                super.close();
                executor.shutdown();
            }
        }, injector, factory);
    }

    private TimerScheduler(Scheduler<I, Instant> scheduler, Consumer<Scheduler<I, TimeoutMetaData>> injector, TimerFactory<I, V> factory) {
        super(scheduler.map(TimeoutMetaData::getNextTimeout));
        this.factory = factory;
        injector.accept(this);
    }

    @Override
    public void schedule(I id) {
        V value = this.factory.getMetaDataFactory().findValue(id);
        if (value != null) {
            this.schedule(Map.entry(new InfinispanTimerMetaDataKey<>(id), value));
        }
    }

    @Override
    public void schedule(Map.Entry<TimerMetaDataKey<I>, V> entry) {
        this.scheduleValue(entry.getKey().getId(), entry.getValue());
    }

    private void scheduleValue(I id, V value) {
        this.schedule(id, this.factory.getMetaDataFactory().createImmutableTimerMetaData(value));
    }

    private static class InvokeTask<I, V> implements Predicate<I>, Consumer<Scheduler<I, TimeoutMetaData>> {
        private final TimerFactory<I, V> factory;
        private final TimerManager<I> manager;
        private final Supplier<Locality> locality;
        private final ScheduledEntries<I, Instant> entries;
        private final TimerRegistry<I> registry;
        private final ExecutorService executor;
        private Scheduler<I, TimeoutMetaData> scheduler;

        InvokeTask(TimerFactory<I, V> factory, TimerManager<I> manager, Supplier<Locality> locality, ScheduledEntries<I, Instant> entries, TimerRegistry<I> registry, ExecutorService executor) {
            this.factory = factory;
            this.manager = manager;
            this.locality = locality;
            this.entries = entries;
            this.registry = registry;
            this.executor = executor;
        }

        @Override
        public void accept(Scheduler<I, TimeoutMetaData> scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public boolean test(I id) {
            TimerFactory<I, V> factory = this.factory;
            TimerManager<I> manager = this.manager;
            Supplier<Locality> locality = this.locality;
            ScheduledEntries<I, Instant> entries = this.entries;
            TimerRegistry<I> registry = this.registry;
            Scheduler<I, TimeoutMetaData> scheduler = this.scheduler;
            TimerMetaDataKey<I> key = new InfinispanTimerMetaDataKey<>(id);
            // Ensure timer is owned by local member
            if (!locality.get().isLocal(key)) {
                InfinispanEjbLogger.ROOT_LOGGER.debugf("Skipping timeout processing of non-local timer %s", id);
                return true;
            }
            Callable<Boolean> task = new Callable<>() {
                @Override
                public Boolean call() throws Exception {
                    InfinispanEjbLogger.ROOT_LOGGER.debugf("Initiating timeout for timer %s", id);
                    TimerMetaDataFactory<I, V> metaDataFactory = factory.getMetaDataFactory();
                    try (Batch batch = manager.getBatchFactory().get()) {
                        V value = metaDataFactory.findValue(id);
                        if (value == null) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer not found %s", id);
                            return true;
                        }

                        TimerMetaData metaData = metaDataFactory.createTimerMetaData(id, value);
                        Optional<Instant> currentTimeoutReference = metaData.getNextTimeout();

                        // Safeguard : ensure timeout was not already triggered elsewhere
                        if (currentTimeoutReference.isEmpty()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Unexpected timeout event triggered for %s", id);
                            return false;
                        }

                        Instant now = Instant.now();
                        Instant currentTimeout = currentTimeoutReference.get();
                        if (currentTimeout.isAfter(now)) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout for timer %s initiated prematurely @ %s", id, currentTimeout);
                            return false;
                        }

                        // Capture previous last timeout in case we need to reset it
                        Optional<Instant> originalLastTimeout = metaData.getLastTimeout();
                        // Record new last timeout - expected to be set prior to triggering timeout
                        metaData.setLastTimeout(currentTimeout);

                        // Determine next timeout
                        Optional<Instant> nextTimeout = metaData.getNextTimeout();
                        // WFLY-19361: If next timeout is in the past do not trigger event
                        if (nextTimeout.orElse(now).isBefore(now)) {
                            // This has the effect of consolidating missed timeouts into a single event
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Skipping notification of missed timeout for timer %s @ %s", id, currentTimeout);
                        } else {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Triggering timeout for timer %s @ %s", id, currentTimeout);

                            Timer<I> timer = factory.createTimer(id, metaData, manager, scheduler);
                            try {
                                timer.invoke();
                            } catch (ExecutionException e) {
                                // Log error and proceed as if it was successful
                                InfinispanEjbLogger.ROOT_LOGGER.error(e.getLocalizedMessage(), e);
                            } catch (RejectedExecutionException e) {
                                // Component is not started or is suspended
                                InfinispanEjbLogger.ROOT_LOGGER.debugf("EJB component is suspended - could not invoke timeout for timer %s", id);
                                // Reset last timeout
                                metaData.setLastTimeout(originalLastTimeout.orElse(null));
                                batch.discard();
                                return false;
                            }

                            // If timeout callback canceled this timer
                            if (timer.isCanceled()) {
                                InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout callback canceled timer %s", id);
                                return true;
                            }
                        }

                        if (nextTimeout.isEmpty()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer %s has expired", id);
                            registry.unregister(id);
                            factory.getMetaDataFactory().remove(id);
                            return true;
                        }

                        // Only reschedule if timer is still local
                        if (!locality.get().isLocal(key)) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer %s is no longer local", id);
                            return true;
                        }

                        // Reschedule using next timeout
                        InfinispanEjbLogger.ROOT_LOGGER.debugf("Rescheduling timer %s for next timeout %s", id, nextTimeout.get());
                        entries.add(id, nextTimeout.get());
                        return false;
                    }
                }
            };
            try {
                Future<Boolean> result = this.executor.submit(task);
                return result.get();
            } catch (RejectedExecutionException e) {
                // Scheduler was shutdown
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Timer was canceled by the scheduler
                return false;
            } catch (ExecutionException e) {
                InfinispanEjbLogger.ROOT_LOGGER.info(e.getLocalizedMessage(), e);
                return false;
            }
        }
    }
}
