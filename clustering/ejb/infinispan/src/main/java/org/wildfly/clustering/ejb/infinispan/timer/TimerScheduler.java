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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
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
public class TimerScheduler<I, C> extends AbstractCacheEntryScheduler<I, ImmutableTimerMetaData> {

    private final TimerFactory<I, C> factory;

    public TimerScheduler(TimerFactory<I, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry) {
        this(factory, manager, locality, closeTimeout, registry, new SortedScheduledEntries<>(), Executors.newSingleThreadExecutor(new DefaultThreadFactory(TimerScheduler.class)));
    }

    private TimerScheduler(TimerFactory<I, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, Duration closeTimeout, TimerRegistry<I> registry, ScheduledEntries<I, Instant> entries, ExecutorService executor) {
        this(entries, new InvokeTask<>(factory, manager, locality, entries, registry, executor), closeTimeout, registry, executor, factory);
    }

    private <T extends Predicate<I> & Consumer<Scheduler<I, ImmutableTimerMetaData>>> TimerScheduler(ScheduledEntries<I, Instant> entries, T invokeTask, Duration closeTimeout, TimerRegistry<I> registry, ExecutorService executor, TimerFactory<I, C> factory) {
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

    private TimerScheduler(Scheduler<I, Instant> scheduler, Consumer<Scheduler<I, ImmutableTimerMetaData>> injector, TimerFactory<I, C> factory) {
        super(scheduler, ImmutableTimerMetaData::getNextTimeout);
        this.factory = factory;
        injector.accept(this);
    }

    @Override
    public void schedule(I id) {
        TimerMetaDataFactory<I, C> metaDataFactory = this.factory.getMetaDataFactory();
        Map.Entry<TimerCreationMetaData<C>, TimerAccessMetaData> entry = metaDataFactory.findValue(id);
        if (entry != null) {
            ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
            this.schedule(id, metaData);
        }
    }

    private static class InvokeTask<I, C> implements Predicate<I>, Consumer<Scheduler<I, ImmutableTimerMetaData>> {
        private final TimerFactory<I, C> factory;
        private final TimerManager<I, TransactionBatch> manager;
        private final Supplier<Locality> locality;
        private final ScheduledEntries<I, Instant> entries;
        private final TimerRegistry<I> registry;
        private final ExecutorService executor;
        private Scheduler<I, ImmutableTimerMetaData> scheduler;

        InvokeTask(TimerFactory<I, C> factory, TimerManager<I, TransactionBatch> manager, Supplier<Locality> locality, ScheduledEntries<I, Instant> entries, TimerRegistry<I> registry, ExecutorService executor) {
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
            TimerFactory<I, C> factory = this.factory;
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
                    TimerMetaDataFactory<I, C> metaDataFactory = factory.getMetaDataFactory();
                    try (TransactionBatch batch = manager.getBatcher().createBatch()) {
                        Map.Entry<TimerCreationMetaData<C>, TimerAccessMetaData> entry = metaDataFactory.findValue(id);
                        if (entry == null) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer not found %s", id);
                            return true;
                        }

                        TimerMetaData metaData = metaDataFactory.createTimerMetaData(id, entry);
                        Instant currentTimeout = metaData.getNextTimeout();

                        // Safeguard : ensure timeout was not already triggered elsewhere
                        if (currentTimeout.isAfter(Instant.now())) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout for timer %s initiated prematurely.", id);
                            return false;
                        }

                        Timer<I> timer = factory.createTimer(id, metaData, manager, scheduler);

                        InfinispanEjbLogger.ROOT_LOGGER.debugf("Triggering timeout for timer %s [%s]", id, timer.getMetaData().getContext());

                        // In case we need to reset the last timeout
                        Instant lastTimeout = metaData.getLastTimout();
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
                            metaData.setLastTimout(lastTimeout);
                            return false;
                        }

                        // If timeout callback canceled this timer
                        if (timer.isCanceled()) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timeout callback canceled timer %s", id);
                            return true;
                        }

                        // Determine next timeout
                        Instant nextTimeout = metaData.getNextTimeout();
                        if (nextTimeout == null) {
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
                        entries.add(id, nextTimeout);
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
