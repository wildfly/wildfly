/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.resilience4j.retry.RetryConfig;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
import org.wildfly.clustering.ejb.cache.timer.ImmutableTimerMetaDataFactory;
import org.wildfly.clustering.ejb.cache.timer.IntervalTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.RemappableTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.ScheduleTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactory;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntryScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerConfiguration;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleCommand;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleLocalKeysTask;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithPersistentMetaDataCommand;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.server.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.server.manager.IdentifierFactory;
import org.wildfly.clustering.server.scheduler.Scheduler;

/**
 * A timer manager backed by an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanTimerManager<I, C> implements TimerManager<I> {

    private final Cache<Key<I>, ?> cache;
    private final CacheProperties properties;
    private final RetryConfig retryConfig;
    private final TimerFactory<I, RemappableTimerMetaDataEntry<C>> factory;
    private final Marshaller<Object, C> marshaller;
    private final IdentifierFactory<I> identifierFactory;
    private final Supplier<Batch> batchFactory;
    private final CacheContainerCommandDispatcherFactory dispatcherFactory;
    private final TimerRegistry<I> registry;

    private volatile Scheduler<I, ImmutableTimerMetaData> scheduledTimers;
    private volatile Scheduler<I, ImmutableTimerMetaData> scheduler;
    private volatile ListenerRegistration schedulerListenerRegistration;

    public InfinispanTimerManager(InfinispanTimerManagerConfiguration<I, C> config) {
        this.cache = config.getCache();
        this.properties = config.getCacheProperties();
        this.retryConfig = config.getRetryConfig();
        this.marshaller = config.getMarshaller();
        this.identifierFactory = new AffinityIdentifierFactory<>(config.getIdentifierFactory(), this.cache);
        this.batchFactory = config.getBatchFactory();
        this.dispatcherFactory = config.getCommandDispatcherFactory();
        this.factory = config.getTimerFactory();
        this.registry = config.getRegistry();
    }

    @Override
    public boolean isStarted() {
        return this.identifierFactory.isStarted();
    }

    @Override
    public void start() {
        Supplier<Locality> locality = () -> Locality.forCurrentConsistentHash(this.cache);

        TimerScheduler<I, RemappableTimerMetaDataEntry<C>> localScheduler = new TimerScheduler<>(this.cache.getName(), this.factory, this, locality, Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout()), this.registry);
        this.scheduledTimers = localScheduler;

        CacheContainerGroup group = this.dispatcherFactory.getGroup();
        this.scheduler = group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(new PrimaryOwnerSchedulerConfiguration<>() {
            @Override
            public String getName() {
                return InfinispanTimerManager.this.cache.getName();
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return InfinispanTimerManager.this.dispatcherFactory;
            }

            @Override
            public CacheEntryScheduler<I, ImmutableTimerMetaData> getScheduler() {
                return localScheduler;
            }

            @Override
            public Function<I, CacheContainerGroupMember> getAffinity() {
                return new UnaryGroupMemberAffinity<>(InfinispanTimerManager.this.cache, group);
            }

            @Override
            public BiFunction<I, ImmutableTimerMetaData, ScheduleCommand<I, ImmutableTimerMetaData>> getScheduleCommandFactory() {
                return InfinispanTimerManager.this.properties.isTransactional() ? ScheduleWithPersistentMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new;
            }

            @Override
            public RetryConfig getRetryConfig() {
                return InfinispanTimerManager.this.retryConfig;
            }
        });

        TimerRegistry<I> registry = this.registry;
        BiConsumer<Locality, Locality> scheduleTask = new ScheduleLocalKeysTask<>(this.cache, TimerMetaDataKeyFilter.INSTANCE, new Consumer<I>() {
            @Override
            public void accept(I id) {
                localScheduler.schedule(id);
                registry.register(id);
            }
        });

        this.schedulerListenerRegistration = new SchedulerTopologyChangeListener<>(this.cache, localScheduler, scheduleTask).register();

        scheduleTask.accept(Locality.of(false), Locality.forCurrentConsistentHash(this.cache));

        this.identifierFactory.start();
    }

    @Override
    public void stop() {
        this.identifierFactory.stop();

        ListenerRegistration registration = this.schedulerListenerRegistration;
        if (registration != null) {
            registration.close();
        }

        Scheduler<I, ImmutableTimerMetaData> scheduler = this.scheduler;
        if (scheduler != null) {
            scheduler.close();
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
        Timer<I> timer = this.factory.createTimer(id, metaData, this, this.scheduledTimers);
        return timer;
    }

    @Override
    public Timer<I> getTimer(I id) {
        ImmutableTimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> metaDataFactory = this.factory.getMetaDataFactory();
        RemappableTimerMetaDataEntry<C> entry = metaDataFactory.findValue(id);
        if (entry != null) {
            ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
            return this.factory.createTimer(id, metaData, this, this.scheduledTimers);
        }
        return null;
    }

    @Override
    public Stream<I> getActiveTimers() {
        // The primary owner scheduler can miss entries, if called during a concurrent topology change event
        return this.dispatcherFactory.getGroup().isSingleton() ? this.scheduledTimers.stream() : this.cache.keySet().stream().filter(TimerMetaDataKeyFilter.INSTANCE).map(Key::getId);
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
