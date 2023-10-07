/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.time.Instant;
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

import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.ScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.infinispan.distribution.Locality;

/**
 * @author Paul Ferraro
 */
public class TimerScheduler<I, V, C> extends AbstractCacheEntryScheduler<I, ImmutableTimerMetaData> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(TimerScheduler.class);

    private final TimerFactory<I, V, C> factory;

    public TimerScheduler(TimerFactory<I, V, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry) {
        this(factory, manager, locality, closeTimeout, registry, new SortedScheduledEntries<>(), Executors.newSingleThreadExecutor(THREAD_FACTORY));
    }

    private TimerScheduler(TimerFactory<I, V, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry, ScheduledEntries<I, Instant> entries, ExecutorService executor) {
        this(entries, new InvokeTask<>(factory, manager, locality, entries, registry, executor), closeTimeout, registry, executor, factory);
    }

    private <T extends Predicate<I> & Consumer<Scheduler<I, ImmutableTimerMetaData>>> TimerScheduler(ScheduledEntries<I, Instant> entries, T invokeTask, Duration closeTimeout, TimerRegistry<I> registry, ExecutorService executor, TimerFactory<I, V, C> factory) {
        this(new LocalScheduler<>(entries, invokeTask, closeTimeout) {
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
        }, invokeTask, factory);
    }

    private TimerScheduler(Scheduler<I, Instant> scheduler, Consumer<Scheduler<I, ImmutableTimerMetaData>> injector, TimerFactory<I, V, C> factory) {
        super(scheduler, ImmutableTimerMetaData::getNextTimeout);
        this.factory = factory;
        injector.accept(this);
    }

    @Override
    public void schedule(I id) {
        TimerMetaDataFactory<I, V, C> metaDataFactory = this.factory.getMetaDataFactory();
        V value = metaDataFactory.findValue(id);
        if (value != null) {
            ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(value);
            this.schedule(id, metaData);
        }
    }

    private static class InvokeTask<I, V, C> implements Predicate<I>, Consumer<Scheduler<I, ImmutableTimerMetaData>> {
        private final TimerFactory<I, V, C> factory;
        private final TimerManager<I, TransactionBatch> manager;
        private final Supplier<Locality> locality;
        private final ScheduledEntries<I, Instant> entries;
        private final TimerRegistry<I> registry;
        private final ExecutorService executor;
        private Scheduler<I, ImmutableTimerMetaData> scheduler;

        InvokeTask(TimerFactory<I, V, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, ScheduledEntries<I, Instant> entries, TimerRegistry<I> registry, ExecutorService executor) {
            this.factory = factory;
            this.manager = manager;
            this.locality = locality;
            this.entries = entries;
            this.registry = registry;
            this.executor = executor;
        }

        @Override
        public void accept(Scheduler<I, ImmutableTimerMetaData> scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public boolean test(I id) {
            TimerFactory<I, V, C> factory = this.factory;
            TimerManager<I, TransactionBatch> manager = this.manager;
            Supplier<Locality> locality = this.locality;
            ScheduledEntries<I, Instant> entries = this.entries;
            TimerRegistry<I> registry = this.registry;
            Scheduler<I, ImmutableTimerMetaData> scheduler = this.scheduler;
            // Ensure timer is owned by local member
            if (!locality.get().isLocal(new GroupedKey<>(id))) {
                InfinispanEjbLogger.ROOT_LOGGER.debugf("Skipping timeout processing of non-local timer %s", id);
                return true;
            }
            Callable<Boolean> task = new Callable<>() {
                @Override
                public Boolean call() throws Exception {
                    InfinispanEjbLogger.ROOT_LOGGER.debugf("Initiating timeout for timer %s", id);
                    TimerMetaDataFactory<I, V, C> metaDataFactory = factory.getMetaDataFactory();
                    try (TransactionBatch batch = manager.getBatcher().createBatch()) {
                        V value = metaDataFactory.findValue(id);
                        if (value == null) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer not found %s", id);
                            return true;
                        }

                        TimerMetaData metaData = metaDataFactory.createTimerMetaData(id, value);
                        Optional<Instant> currentTimeoutReference = metaData.getNextTimeout();

                        // Safeguard : ensure timeout was not already triggered elsewhere
                        if (currentTimeoutReference.isEmpty()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Unexpected timeout event triggered.", id);
                            return false;
                        }
                        Instant currentTimeout = currentTimeoutReference.get();
                        if (currentTimeout.isAfter(Instant.now())) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout for timer %s initiated prematurely.", id);
                            return false;
                        }

                        Timer<I> timer = factory.createTimer(id, metaData, manager, scheduler);

                        InfinispanEjbLogger.ROOT_LOGGER.debugf("Triggering timeout for timer %s [%s]", id, timer.getMetaData().getContext());

                        // In case we need to reset the last timeout
                        Optional<Instant> lastTimeout = metaData.getLastTimout();
                        // Record last timeout - expected to be set prior to triggering timeout
                        metaData.setLastTimout(currentTimeout);

                        try {
                            timer.invoke();
                        } catch (ExecutionException e) {
                            // Log error and proceed as if it was successful
                            InfinispanEjbLogger.ROOT_LOGGER.error(e.getLocalizedMessage(), e);
                        } catch (RejectedExecutionException e) {
                            // Component is not started or is suspended
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("EJB component is suspended - could not invoke timeout for timer %s", id);
                            // Reset last timeout
                            metaData.setLastTimout(lastTimeout.orElse(null));
                            return false;
                        }

                        // If timeout callback canceled this timer
                        if (timer.isCanceled()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout callback canceled timer %s", id);
                            return true;
                        }

                        // Determine next timeout
                        Optional<Instant> nextTimeout = metaData.getNextTimeout();
                        if (nextTimeout.isEmpty()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer %s has expired", id);
                            registry.unregister(id);
                            factory.getMetaDataFactory().remove(id);
                            return true;
                        }

                        // Only reschedule if timer is still local
                        if (!locality.get().isLocal(new GroupedKey<>(id))) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer %s is no longer local", id);
                            return true;
                        }

                        // Reschedule using next timeout
                        InfinispanEjbLogger.ROOT_LOGGER.debugf("Rescheduling timer %s for next timeout %s", id, nextTimeout);
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
