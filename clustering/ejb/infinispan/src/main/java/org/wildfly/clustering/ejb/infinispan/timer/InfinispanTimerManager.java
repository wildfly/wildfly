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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.affinity.AffinityIdentifierFactory;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleLocalKeysTask;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerTopologyChangeListener;
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
import org.wildfly.clustering.infinispan.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.distribution.Locality;
import org.wildfly.clustering.infinispan.distribution.SimpleLocality;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * A timer manager backed by an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanTimerManager<I, C> implements TimerManager<I, TransactionBatch> {

    private final Cache<Key<I>, ?> cache;
    private final CacheProperties properties;
    private final TimerFactory<I, RemappableTimerMetaDataEntry<C>, C> factory;
    private final Marshaller<Object, C> marshaller;
    private final IdentifierFactory<I> identifierFactory;
    private final Batcher<TransactionBatch> batcher;
    private final CommandDispatcherFactory dispatcherFactory;
    private final Group<Address> group;
    private final TimerRegistry<I> registry;

    private volatile Scheduler<I, ImmutableTimerMetaData> scheduledTimers;
    private volatile Scheduler<I, ImmutableTimerMetaData> scheduler;
    private volatile ListenerRegistration schedulerListenerRegistration;

    public InfinispanTimerManager(InfinispanTimerManagerConfiguration<I, C> config) {
        this.cache = config.getCache();
        this.properties = config.getCacheProperties();
        this.marshaller = config.getMarshaller();
        this.identifierFactory = new AffinityIdentifierFactory<>(config.getIdentifierFactory(), this.cache, config.getKeyAffinityServiceFactory());
        this.batcher = config.getBatcher();
        this.dispatcherFactory = config.getCommandDispatcherFactory();
        this.group = config.getGroup();
        this.factory = config.getTimerFactory();
        this.registry = config.getRegistry();
    }

    @Override
    public void start() {
        Supplier<Locality> locality = () -> new CacheLocality(this.cache);

        TimerScheduler<I, RemappableTimerMetaDataEntry<C>, C> localScheduler = new TimerScheduler<>(this.factory, this, locality, Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout()), this.registry);
        this.scheduledTimers = localScheduler;

        this.scheduler = this.group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(this.dispatcherFactory, this.cache.getName(), localScheduler, new PrimaryOwnerLocator<>(this.cache, this.group), InfinispanTimerMetaDataKey::new, this.properties.isTransactional() ? ScheduleWithMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new);

        TimerRegistry<I> registry = this.registry;
        BiConsumer<Locality, Locality> scheduleTask = new ScheduleLocalKeysTask<>(this.cache, TimerMetaDataKeyFilter.INSTANCE, new Consumer<I>() {
            @Override
            public void accept(I id) {
                localScheduler.schedule(id);
                registry.register(id);
            }
        });

        this.schedulerListenerRegistration = new SchedulerTopologyChangeListener<>(this.cache, localScheduler, scheduleTask).register();

        scheduleTask.accept(new SimpleLocality(false), new CacheLocality(this.cache));

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
        TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>, C> metaDataFactory = this.factory.getMetaDataFactory();
        if (metaDataFactory.createValue(id, new AbstractMap.SimpleImmutableEntry<>(entry, index)) == null) return null; // Timer with index already exists

        ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
        Timer<I> timer = this.factory.createTimer(id, metaData, this, this.scheduledTimers);
        return timer;
    }

    @Override
    public Timer<I> getTimer(I id) {
        ImmutableTimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>, C> metaDataFactory = this.factory.getMetaDataFactory();
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
        return this.group.isSingleton() ? this.scheduledTimers.stream() : this.cache.keySet().stream().filter(TimerMetaDataKeyFilter.INSTANCE).map(Key::getId);
    }

    @Override
    public Supplier<I> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public String toString() {
        return this.cache.getName();
    }
}
