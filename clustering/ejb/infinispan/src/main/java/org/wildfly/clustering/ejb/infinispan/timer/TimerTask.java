/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
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
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The timer invocation task
 * @author Paul Ferraro
 */
public class TimerTask<I, V> implements Predicate<I>, AutoCloseable {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(TimerTask.class, WildFlySecurityManager.getClassLoaderPrivileged(TimerTask.class));

    interface Configuration<I, V> {
        TimerFactory<I, V> getTimerFactory();
        TimerManager<I> getTimerManager();
        Locality getLocality();
        Scheduler<I, TimeoutMetaData> getScheduler();
        ScheduledEntries<I, Instant> getScheduledEntries();
        TimerRegistry<I> getTimerRegistry();
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private final Configuration<I, V> configuration;

    TimerTask(Configuration<I, V> configuration) {
        this.configuration = configuration;
    }

    @Override
    public void close() {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                TimerTask.this.executor.shutdown();
                return null;
            }
        });
    }

    @Override
    public boolean test(I id) {
        TimerFactory<I, V> timerFactory = this.configuration.getTimerFactory();
        TimerMetaDataFactory<I, V> metaDataFactory = timerFactory.getMetaDataFactory();
        Scheduler<I, TimeoutMetaData> scheduler = this.configuration.getScheduler();
        TimerManager<I> manager = this.configuration.getTimerManager();
        TimerRegistry<I> registry = this.configuration.getTimerRegistry();
        ScheduledEntries<I, Instant> entries = this.configuration.getScheduledEntries();
        Supplier<Locality> localityProvider = this.configuration::getLocality;
        TimerMetaDataKey<I> key = new InfinispanTimerMetaDataKey<>(id);
        // Ensure timer is owned by local member
        if (!localityProvider.get().isLocal(key)) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Skipping timeout processing of non-local timer %s", id);
            return true;
        }
        Callable<Boolean> task = new Callable<>() {
            @Override
            public Boolean call() {
                InfinispanEjbLogger.ROOT_LOGGER.debugf("Initiating timeout for timer %s", id);
                try (Batch batch = manager.getBatchFactory().get()) {
                    try {
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

                            Timer<I> timer = timerFactory.createTimer(id, metaData, manager, scheduler);
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
                            metaDataFactory.remove(id);
                            return true;
                        }

                        // Only reschedule if timer is still local
                        if (!localityProvider.get().isLocal(key)) {
                            InfinispanEjbLogger.ROOT_LOGGER.debugf("Timer %s is no longer local", id);
                            return true;
                        }

                        // Reschedule using next timeout
                        InfinispanEjbLogger.ROOT_LOGGER.debugf("Rescheduling timer %s for next timeout %s", id, nextTimeout.get());
                        entries.add(id, nextTimeout.get());
                        return false;
                    } catch (RuntimeException | Error e) {
                        batch.discard();
                        throw e;
                    }
                }
            }
        };
        try {
            // Use isolated thread context
            return this.executor.submit(task).get();
        } catch (RejectedExecutionException e) {
            // Scheduler was shutdown
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Timer was canceled by the scheduler (should not happen)
            return false;
        } catch (ExecutionException e) {
            InfinispanEjbLogger.ROOT_LOGGER.info(e.getLocalizedMessage(), e);
            return false;
        }
    }
}
