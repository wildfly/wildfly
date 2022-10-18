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

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
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
public class InfinispanTimerManager<I, V> implements TimerManager<I, TransactionBatch> {

    private final Cache<Key<I>, ?> cache;
    private final CacheProperties properties;
    private final TimerFactory<I, V> factory;
    private final Marshaller<Object, V> marshaller;
    private final IdentifierFactory<I> identifierFactory;
    private final Batcher<TransactionBatch> batcher;
    private final CommandDispatcherFactory dispatcherFactory;
    private final Group<Address> group;
    private final TimerRegistry<I> registry;

    private volatile Scheduler<I, ImmutableTimerMetaData> scheduledTimers;
    private volatile Scheduler<I, ImmutableTimerMetaData> scheduler;
    private volatile ListenerRegistration schedulerListenerRegistration;

    public InfinispanTimerManager(InfinispanTimerManagerConfiguration<I, V> config) {
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

        TimerScheduler<I, V> localScheduler = new TimerScheduler<>(this.factory, this, locality, Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout()), this.registry);
        this.scheduledTimers = localScheduler;

        this.scheduler = this.group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(this.dispatcherFactory, this.cache.getName(), localScheduler, new PrimaryOwnerLocator<>(this.cache, this.group), TimerCreationMetaDataKey::new, this.properties.isTransactional() ? ScheduleWithMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new);

        TimerRegistry<I> registry = this.registry;
        BiConsumer<Locality, Locality> scheduleTask = new ScheduleLocalKeysTask<>(this.cache, TimerCreationMetaDataKeyFilter.INSTANCE, new Consumer<I>() {
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
            TimerCreationMetaData<V> creationMetaData = new IntervalTimerCreationMetaDataEntry<>(this.marshaller.write(context), config);
            return this.createTimer(id, creationMetaData, (TimerIndex) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context) {
        try {
            TimerCreationMetaData<V> creationMetaData = new ScheduleTimerCreationMetaDataEntry<>(this.marshaller.write(context), config, null);
            return this.createTimer(id, creationMetaData, (TimerIndex) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context, Method method, int index) {
        try {
            TimerCreationMetaData<V> creationMetaData = new ScheduleTimerCreationMetaDataEntry<>(this.marshaller.write(context), config, method);
            return this.createTimer(id, creationMetaData, new TimerIndex(method, index));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Timer<I> createTimer(I id, TimerCreationMetaData<V> creationMetaData, TimerIndex index) {
        TimerMetaDataFactory<I, V> metaDataFactory = this.factory.getMetaDataFactory();
        Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> entry = metaDataFactory.createValue(id, new SimpleImmutableEntry<>(creationMetaData, index));
        if (entry == null) return null; // Timer with index already exists

        ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
        Timer<I> timer = this.factory.createTimer(id, metaData, this, this.scheduledTimers);
        return timer;
    }

    @Override
    public Timer<I> getTimer(I id) {
        ImmutableTimerMetaDataFactory<I, V> metaDataFactory = this.factory.getMetaDataFactory();
        Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> entry = metaDataFactory.findValue(id);
        if (entry != null) {
            ImmutableTimerMetaData metaData = metaDataFactory.createImmutableTimerMetaData(entry);
            return this.factory.createTimer(id, metaData, this, this.scheduledTimers);
        }
        return null;
    }

    @Override
    public Stream<I> getActiveTimers() {
        return this.scheduler.stream();
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
