/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.CacheStreamFilter;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
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
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactory;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntriesTask;
import org.wildfly.clustering.server.infinispan.scheduler.CacheKeysTask;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerConfiguration;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleCommand;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.server.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.server.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.server.manager.IdentifierFactory;

import io.github.resilience4j.retry.RetryConfig;

/**
 * A timer manager backed by an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanTimerManager<I, C> implements TimerManager<I> {

    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> cache;
    private final TimerFactory<I, RemappableTimerMetaDataEntry<C>> factory;
    private final Marshaller<Object, C> marshaller;
    private final IdentifierFactory<I> identifierFactory;
    private final AtomicBoolean identifierFactoryStarted = new AtomicBoolean(false);
    private final Supplier<Batch> batchFactory;
    private final TimerRegistry<I> registry;
    private final Scheduler<I, TimeoutMetaData> inactiveScheduler = Scheduler.inactive();
    private final Scheduler<I, TimeoutMetaData> scheduler;
    private final AtomicReference<Scheduler<I, TimeoutMetaData>> schedulerReference = new AtomicReference<>(this.inactiveScheduler);
    private final AtomicReference<ListenerRegistration> schedulerListenerRegistration = new AtomicReference<>();

    public InfinispanTimerManager(InfinispanTimerManagerConfiguration<I, C> config) {
        this.cache = config.getCache();
        this.marshaller = config.getMarshaller();
        this.identifierFactory = new AffinityIdentifierFactory<>(config.getIdentifierFactory(), this.cache);
        this.batchFactory = config.getBatchFactory();
        this.factory = config.getTimerFactory();
        this.registry = config.getRegistry();

        CacheProperties properties = config.getCacheProperties();
        RetryConfig retryConfig = config.getRetryConfig();
        CacheContainerCommandDispatcherFactory dispatcherFactory = config.getCommandDispatcherFactory();
        CacheContainerGroup group = dispatcherFactory.getGroup();
        Scheduler<I, TimeoutMetaData> localScheduler = Scheduler.fromReference(this.schedulerReference::get);
        this.scheduler = group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(new PrimaryOwnerSchedulerConfiguration<>() {
            @Override
            public String getName() {
                return config.getCache().getName();
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return dispatcherFactory;
            }

            @Override
            public Scheduler<I, TimeoutMetaData> getScheduler() {
                return localScheduler;
            }

            @Override
            public Function<I, CacheContainerGroupMember> getAffinity() {
                return new UnaryGroupMemberAffinity<>(config.getCache(), group);
            }

            @Override
            public BiFunction<I, TimeoutMetaData, ScheduleCommand<I, TimeoutMetaData>> getScheduleCommandFactory() {
                return properties.isTransactional() ? ScheduleWithPersistentTimeoutMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new;
            }

            @Override
            public RetryConfig getRetryConfig() {
                return retryConfig;
            }
        });

        // If cache is not suspended, start the affinity service now so that any timers created before start() will hash locally
        if (properties.isActive()) {
            this.identifierFactory.start();
            this.identifierFactoryStarted.set(true);
        }
    }

    @Override
    public boolean isStarted() {
        return this.identifierFactory.isStarted();
    }

    @Override
    public void start() {
        Supplier<Locality> locality = () -> Locality.forCurrentConsistentHash(this.cache);

        TimerScheduler<I, RemappableTimerMetaDataEntry<C>> localScheduler = new TimerScheduler<>(this.cache.getName(), this.factory, this, locality, Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout()), this.registry);
        try (Scheduler<I, TimeoutMetaData> scheduler = this.schedulerReference.getAndSet(localScheduler)) {
            // auto-close
        }

        Consumer<CacheStreamFilter<Map.Entry<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>>>> scheduleTask = new CacheEntriesTask<>(this.cache, TimerCacheEntryFilter.META_DATA_ENTRY.cast(), localScheduler::schedule);
        org.wildfly.clustering.function.Consumer<I> cancel = localScheduler::cancel;
        Consumer<CacheStreamFilter<TimerMetaDataKey<I>>> cancelTask = new CacheKeysTask<>(this.cache, TimerCacheKeyFilter.META_DATA_KEY, cancel.compose(Key::getId));

        this.schedulerListenerRegistration.set(new SchedulerTopologyChangeListener<>(this.cache, scheduleTask, cancelTask).register());

        scheduleTask.accept(CacheStreamFilter.local(this.cache));

        if (this.identifierFactoryStarted.compareAndSet(false, true)) {
            this.identifierFactory.start();
        }
    }

    @Override
    public void stop() {
        if (this.identifierFactoryStarted.compareAndSet(true, false)) {
            this.identifierFactory.stop();
        }

        try (ListenerRegistration registration = this.schedulerListenerRegistration.getAndSet(null)) {
            // auto-closed
        }

        try (Scheduler<I, TimeoutMetaData> scheduler = this.schedulerReference.getAndSet(this.inactiveScheduler)) {
            // Switch to inactive scheduler
        }
    }

    @Override
    public void close() {
        this.scheduler.close();
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
