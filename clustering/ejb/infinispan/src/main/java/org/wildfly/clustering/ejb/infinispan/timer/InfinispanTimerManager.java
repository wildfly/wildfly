/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.CacheStreamFilter;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ejb.cache.timer.ImmutableTimerMetaDataFactory;
import org.wildfly.clustering.ejb.cache.timer.IntervalTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.RemappableTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.ScheduleTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactoryService;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntriesTask;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntrySchedulerService;
import org.wildfly.clustering.server.infinispan.scheduler.CacheKeysTask;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerCommand;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerService;
import org.wildfly.clustering.server.local.scheduler.LocalSchedulerService;
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.manager.IdentifierFactoryService;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.clustering.server.scheduler.SchedulerService;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A timer manager backed by an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanTimerManager<I, C> implements TimerManager<I> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(InfinispanTimerManager.class, WildFlySecurityManager.getClassLoaderPrivileged(InfinispanTimerManager.class));

    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> cache;
    private final TimerFactory<I, RemappableTimerMetaDataEntry<C>> factory;
    private final Marshaller<Object, C> marshaller;
    private final IdentifierFactoryService<I> identifierFactory;
    private final Supplier<Batch> batchFactory;
    private final SchedulerService<I, TimeoutMetaData> scheduler;

    public InfinispanTimerManager(InfinispanTimerManagerConfiguration<I, C> config) {
        Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> cache = config.getCache();
        this.cache = cache;
        this.marshaller = config.getMarshaller();
        this.identifierFactory = new AffinityIdentifierFactoryService<>(config.getIdentifierFactory(), this.cache);
        this.batchFactory = config.getBatchFactory();
        this.factory = config.getTimerFactory();

        TimerRegistry<I> registry = config.getRegistry();
        CacheContainerCommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        CacheContainerGroup group = dispatcherFactory.getGroup();
        AtomicReference<Scheduler<I, TimeoutMetaData>> reference = new AtomicReference<>();
        ScheduledEntries<I, Instant> entries = ScheduledEntries.sorted();
        TimerTask<I, RemappableTimerMetaDataEntry<C>> task = new TimerTask<>(new TimerTask.Configuration<I, RemappableTimerMetaDataEntry<C>>() {
            @Override
            public TimerFactory<I, RemappableTimerMetaDataEntry<C>> getTimerFactory() {
                return config.getTimerFactory();
            }

            @Override
            public TimerManager<I> getTimerManager() {
                return InfinispanTimerManager.this;
            }

            @Override
            public Locality getLocality() {
                return Locality.forCurrentConsistentHash(config.getCache());
            }

            @Override
            public Scheduler<I, TimeoutMetaData> getScheduler() {
                return reference.get();
            }

            @Override
            public ScheduledEntries<I, Instant> getScheduledEntries() {
                return entries;
            }

            @Override
            public TimerRegistry<I> getTimerRegistry() {
                return registry;
            }
        });
        @SuppressWarnings("resource")
        SchedulerService<I, Instant> localScheduler = new LocalSchedulerService<>(new LocalSchedulerService.Configuration<I>() {
            @Override
            public String getName() {
                return config.getName();
            }

            @Override
            public ScheduledEntries<I, Instant> getScheduledEntries() {
                return entries;
            }

            @Override
            public Predicate<I> getTask() {
                return task;
            }

            @Override
            public ThreadFactory getThreadFactory() {
                return THREAD_FACTORY;
            }

            @Override
            public Duration getCloseTimeout() {
                return config.getStopTimeout();
            }
        }) {
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
                task.close();
                super.close();
            }
        };
        TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> metaDataFactory = config.getTimerFactory().getMetaDataFactory();
        CacheEntrySchedulerService<I, TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>, TimeoutMetaData> cacheEntryScheduler = new CacheEntrySchedulerService<>(localScheduler.compose(UnaryOperator.identity(), TimeoutMetaData::getNextTimeout), BiFunction.applyLatter(metaDataFactory::createImmutableTimerMetaData)) {
            @Override
            public void start() {
                super.start();
                // Schedule locally-owned entries
                CacheEntriesTask.schedule(cache, TimerCacheEntryFilter.META_DATA_ENTRY.cast(), this).accept(CacheStreamFilter.local(cache));
            }
        };
        reference.set(cacheEntryScheduler);
        this.scheduler = !group.isSingleton() ? new PrimaryOwnerSchedulerService<>(new PrimaryOwnerSchedulerService.Configuration<I, TimeoutMetaData, Map.Entry<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>>, TimerMetaDataKey<I>>() {
            @Override
            public EmbeddedCacheConfiguration getCacheConfiguration() {
                return config;
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return dispatcherFactory;
            }

            @Override
            public SchedulerService<I, TimeoutMetaData> getScheduler() {
                return cacheEntryScheduler;
            }

            @Override
            public Function<Map.Entry<I, TimeoutMetaData>, PrimaryOwnerCommand<I, TimeoutMetaData, Void>> getScheduleCommandFactory() {
                return ScheduleTimeoutCommand::new;
            }

            @Override
            public Consumer<CacheStreamFilter<TimerMetaDataKey<I>>> getCancelTask() {
                return CacheKeysTask.cancel(cache, TimerCacheKeyFilter.META_DATA_KEY, cacheEntryScheduler);
            }

            @Override
            public Consumer<CacheStreamFilter<Map.Entry<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>>>> getScheduleTask() {
                return CacheEntriesTask.schedule(cache, TimerCacheEntryFilter.META_DATA_ENTRY.cast(), cacheEntryScheduler);
            }
        }) : cacheEntryScheduler;

        // If cache is not suspended, start the affinity service now so that any timers created before start() will hash locally
        if (config.isActive()) {
            this.identifierFactory.start();
        }
    }

    @Override
    public boolean isStarted() {
        return this.scheduler.isStarted();
    }

    @Override
    public void start() {
        if (!this.identifierFactory.isStarted()) {
            this.identifierFactory.start();
        }
        this.scheduler.start();
    }

    @Override
    public void stop() {
        this.scheduler.stop();
        this.identifierFactory.stop();
    }

    @Override
    public void close() {
        this.scheduler.close();
        if (this.identifierFactory.isStarted()) {
            this.identifierFactory.stop();
        }
    }

    @Override
    public Timer<I> createTimer(I id, IntervalTimerConfiguration config, Object context) {
        try {
            RemappableTimerMetaDataEntry<C> entry = new IntervalTimerMetaDataEntry<>(this.marshaller.write(context), config);
            return this.createTimer(id, entry, (TimerIndex) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context) {
        try {
            RemappableTimerMetaDataEntry<C> entry = new ScheduleTimerMetaDataEntry<>(this.marshaller.write(context), config);
            return this.createTimer(id, entry, (TimerIndex) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context, Method method, int index) {
        try {
            RemappableTimerMetaDataEntry<C> entry = new ScheduleTimerMetaDataEntry<>(this.marshaller.write(context), config, method);
            return this.createTimer(id, entry, new TimerIndex(method, index));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Timer<I> createTimer(I id, RemappableTimerMetaDataEntry<C> entry, TimerIndex index) {
        TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> metaDataFactory = this.factory.getMetaDataFactory();
        if (metaDataFactory.createValue(id, new AbstractMap.SimpleImmutableEntry<>(entry, index)) == null) return null; // Timer with index already exists

        ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
        Timer<I> timer = this.factory.createTimer(id, metaData, this, this.scheduler);
        return timer;
    }

    @Override
    public Timer<I> getTimer(I id) {
        return this.findTimer(ImmutableTimerMetaDataFactory::findValue, id);
    }

    @Override
    public Timer<I> readTimer(I id) {
        return this.findTimer(ImmutableTimerMetaDataFactory::tryValue, id);
    }

    private Timer<I> findTimer(BiFunction<ImmutableTimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>>, I, RemappableTimerMetaDataEntry<C>> finder, I id) {
        ImmutableTimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> metaDataFactory = this.factory.getMetaDataFactory();
        RemappableTimerMetaDataEntry<C> entry = finder.apply(metaDataFactory, id);
        if (entry != null) {
            ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
            return this.factory.createTimer(id, metaData, this, this.scheduler);
        }
        return null;
    }

    @Override
    public Stream<I> getActiveTimers() {
        return this.cache.keySet().stream().filter(TimerCacheKeyFilter.META_DATA_KEY).map(Key::getId);
    }

    @Override
    public Supplier<I> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Supplier<Batch> getBatchFactory() {
        return this.batchFactory;
    }

    @Override
    public String toString() {
        return this.cache.getName();
    }
}
