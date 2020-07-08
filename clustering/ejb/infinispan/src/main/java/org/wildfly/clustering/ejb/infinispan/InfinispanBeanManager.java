/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.ee.infinispan.tx.InfinispanBatcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.PredicateCacheEventFilter;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
@Listener
public class InfinispanBeanManager<I, T, C> implements BeanManager<I, T, TransactionBatch> {

    private static final String IDLE_TIMEOUT_PROPERTY = "jboss.ejb.stateful.%s.idle-timeout";

    private final String name;
    private final Cache<BeanKey<I>, BeanEntry<I>> cache;
    private final CacheProperties properties;
    private final BeanFactory<I, T> beanFactory;
    private final BeanGroupFactory<I, T, C> groupFactory;
    private final IdentifierFactory<I> identifierFactory;
    private final KeyAffinityService<BeanKey<I>> affinity;
    private final Registry<String, ?> registry;
    private final CommandDispatcherFactory dispatcherFactory;
    private final ExpirationConfiguration<T> expiration;
    private final PassivationConfiguration<T> passivation;
    private final Batcher<TransactionBatch> batcher;
    private final Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> filter;
    private final AtomicReference<Future<?>> rehashFuture = new AtomicReference<>();
    private final AtomicInteger rehashTopology = new AtomicInteger();
    private final Function<BeanKey<I>, Node> primaryOwnerLocator;

    private volatile Scheduler<I, ImmutableBeanEntry<I>> scheduler;
    private volatile ExecutorService executor;
    private volatile org.wildfly.clustering.ee.Scheduler<I, ImmutableBeanEntry<I>> primaryOwnerScheduler;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<I, T> configuration, IdentifierFactory<I> identifierFactory, Configuration<BeanKey<I>, BeanEntry<I>, BeanFactory<I, T>> beanConfiguration, Configuration<BeanGroupKey<I>, BeanGroupEntry<I, T, C>, BeanGroupFactory<I, T, C>> groupConfiguration) {
        this.name = configuration.getName();
        this.filter = configuration.getBeanFilter();
        this.groupFactory = groupConfiguration.getFactory();
        this.beanFactory = beanConfiguration.getFactory();
        this.cache = beanConfiguration.getCache();
        this.properties = configuration.getProperties();
        this.batcher = new InfinispanBatcher(this.cache);
        Address address = this.cache.getCacheManager().getAddress();
        KeyAffinityServiceFactory affinityFactory = configuration.getAffinityFactory();
        KeyGenerator<BeanKey<I>> beanKeyGenerator = () -> beanConfiguration.getFactory().createKey(identifierFactory.createIdentifier());
        this.affinity = affinityFactory.createService(this.cache, beanKeyGenerator);
        this.identifierFactory = () -> this.affinity.getKeyForAddress(address).getId();
        this.registry = configuration.getRegistry();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpirationConfiguration();
        this.passivation = configuration.getPassivationConfiguration();
        this.primaryOwnerLocator = new PrimaryOwnerLocator<>(beanConfiguration.getCache(), configuration.getNodeFactory(), configuration.getRegistry().getGroup());
    }

    @Override
    public void start() {
        this.executor = Executors.newSingleThreadExecutor(new DefaultThreadFactory(InfinispanBeanManager.class));
        this.affinity.start();

        Duration stopTimeout = Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout());
        List<Scheduler<I, ImmutableBeanEntry<I>>> schedulers = new ArrayList<>(2);
        Duration timeout = this.expiration.getTimeout();
        if ((timeout != null) && !timeout.isNegative()) {
            schedulers.add(new BeanExpirationScheduler<>(this.dispatcherFactory.getGroup(), this.batcher, this.beanFactory, this.expiration, new ExpiredBeanRemover<>(this.beanFactory, this.expiration), stopTimeout));
        }

        String dispatcherName = String.join("/", this.cache.getName(), this.filter.toString());

        String globalIdleTimeout = WildFlySecurityManager.getPropertyPrivileged("jboss.ejb.stateful.idle-timeout", null);
        String idleTimeout = WildFlySecurityManager.getPropertyPrivileged(String.format(IDLE_TIMEOUT_PROPERTY, this.name), globalIdleTimeout);
        if (idleTimeout != null) {
            Duration idleDuration = Duration.parse(idleTimeout);
            if (!idleDuration.isNegative()) {
                schedulers.add(new EagerEvictionScheduler<>(this.dispatcherFactory.getGroup(), this.batcher, this.beanFactory, this.groupFactory, idleDuration, this.dispatcherFactory, dispatcherName + "/eager-passivation", stopTimeout));
            }
        }

        this.scheduler = !schedulers.isEmpty() ? new CompositeScheduler<>(schedulers) : null;
        this.primaryOwnerScheduler = (this.scheduler != null) ? (this.dispatcherFactory.getGroup().isSingleton() ? this.scheduler : new PrimaryOwnerScheduler<>(this.dispatcherFactory, dispatcherName, this.scheduler, this.primaryOwnerLocator, InfinispanBeanKey::new)) : null;

        this.cache.addListener(this, new PredicateCacheEventFilter<>(this.filter), null);
        if (this.scheduler != null) {
            new ScheduleExpirationTask<>(this.cache, this.filter, this.scheduler, new SimpleLocality(false), new CacheLocality(this.cache)).run();
        }
    }

    @Override
    public void stop() {
        this.groupFactory.close();
        this.cache.removeListener(this);
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_NOW_ACTION);
        try {
            this.executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (this.primaryOwnerScheduler != null) {
                this.primaryOwnerScheduler.close();
            }
            this.affinity.stop();
        }
    }

    @Override
    public boolean isRemotable(final Throwable throwable) {
        Throwable subject = throwable;
        while (subject != null) {
            if (subject instanceof CacheException) {
                return false;
            }
            subject = subject.getCause();
        }
        return true;
    }

    @Override
    public Affinity getStrictAffinity() {
        Group group = this.registry.getGroup();
        return this.cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(this.registry.getEntry(group.getLocalMember()).getKey());
    }

    @Override
    public Affinity getWeakAffinity(I id) {
        org.infinispan.configuration.cache.Configuration config = this.cache.getCacheConfiguration();
        CacheMode mode = config.clustering().cacheMode();
        if (mode.isClustered()) {
            // Non-transactional invalidation caches map all keys to a single segment - thus should use local affinity
            Node node = (!mode.isInvalidation() || config.transaction().transactionMode().isTransactional()) ? this.locatePrimaryOwner(id) : this.registry.getGroup().getLocalMember();
            Map.Entry<String, ?> entry = this.registry.getEntry(node);
            if (entry != null) {
                return new NodeAffinity(entry.getKey());
            }
        }
        return Affinity.NONE;
    }

    Node locatePrimaryOwner(I id) {
        return this.primaryOwnerLocator.apply(new InfinispanBeanKey<>(id));
    }

    @Override
    public Bean<I, T> createBean(I id, I groupId, T bean) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        BeanGroupEntry<I, T, C> groupEntry = (id == groupId) ? this.groupFactory.createValue(groupId, null) : this.groupFactory.findValue(groupId);
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry);
        group.addBean(id, bean);
        group.releaseBean(id, this.properties.isPersistent() ? this.passivation.getPassivationListener() : null);
        BeanEntry<I> entry = this.beanFactory.createValue(id, groupId);
        return new SchedulableBean<>(this.beanFactory.createBean(id, entry), entry, this.primaryOwnerScheduler);
    }

    @SuppressWarnings("resource")
    @Override
    public Bean<I, T> findBean(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Locating bean %s", id);
        BeanEntry<I> entry = this.beanFactory.findValue(id);
        Bean<I, T> bean = (entry != null) ? this.beanFactory.createBean(id, entry) : null;
        if (bean == null) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        if (bean.isExpired()) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Bean %s was found, but has expired", id);
            this.beanFactory.remove(id, this.expiration.getRemoveListener());
            return null;
        }
        if (this.primaryOwnerScheduler != null) {
            this.primaryOwnerScheduler.cancel(id);
        }
        return new SchedulableBean<>(bean, entry, this.primaryOwnerScheduler);
    }

    @Override
    public boolean containsBean(I id) {
        return this.cache.containsKey(this.beanFactory.createKey(id));
    }

    @Override
    public IdentifierFactory<I> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public int getActiveCount() {
        try (Stream<Map.Entry<BeanKey<I>, BeanEntry<I>>> entries = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).entrySet().stream()) {
            return (int) entries.filter(this.filter).count();
        }
    }

    @Override
    public int getPassiveCount() {
        return this.groupFactory.getPassiveCount();
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<BeanKey<I>, BeanEntry<I>> event) {
        if (this.scheduler != null) {
            Locality newLocality = new ConsistentHashLocality(event.getCache(), event.getConsistentHashAtEnd());
            try {
                if (event.isPre()) {
                    this.rehashTopology.set(event.getNewTopologyId());
                    Future<?> future = this.rehashFuture.getAndSet(null);
                    if (future != null) {
                        future.cancel(true);
                    }
                    this.executor.submit(new CancelExpirationTask<>(this.scheduler, newLocality));
                } else {
                    this.rehashTopology.compareAndSet(event.getNewTopologyId(), 0);
                    Locality oldLocality = new ConsistentHashLocality(event.getCache(), event.getConsistentHashAtStart());
                    this.rehashFuture.set(this.executor.submit(new ScheduleExpirationTask<>(this.cache, this.filter, this.scheduler, oldLocality, newLocality)));
                }
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<BeanKey<I>, BeanEntry<I>> event) {
        if (this.scheduler != null) {
            if (!event.isPre()) {
                // If this topology change has no corresponding rehash event, we must reschedule expirations as primary ownership may have changed
                if (this.rehashTopology.get() != event.getNewTopologyId()) {
                    Future<?> future = this.rehashFuture.getAndSet(null);
                    if (future != null) {
                        future.cancel(true);
                    }
                    Cache<BeanKey<I>, BeanEntry<I>> cache = event.getCache();
                    Locality oldLocality = new ConsistentHashLocality(cache, event.getReadConsistentHashAtStart());
                    Locality newLocality = new ConsistentHashLocality(cache, event.getWriteConsistentHashAtEnd());
                    try {
                        this.rehashFuture.compareAndSet(null, this.executor.submit(new ScheduleExpirationTask<>(cache, this.filter, this.scheduler, oldLocality, newLocality)));
                    } catch (RejectedExecutionException e) {
                        // Executor was shutdown
                    }
                }
            }
        }
    }

    private static class CancelExpirationTask<I> implements Runnable {
        private final Scheduler<I, ImmutableBeanEntry<I>> scheduler;
        private final Locality locality;

        CancelExpirationTask(Scheduler<I, ImmutableBeanEntry<I>> scheduler, Locality locality) {
            this.scheduler = scheduler;
            this.locality = locality;
        }

        @Override
        public void run() {
            this.scheduler.cancel(this.locality);
        }
    }

    private static class ScheduleExpirationTask<I> implements Runnable {
        private final Cache<BeanKey<I>, BeanEntry<I>> cache;
        private final Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> filter;
        private final Scheduler<I, ImmutableBeanEntry<I>> scheduler;
        private final Locality oldLocality;
        private final Locality newLocality;

        ScheduleExpirationTask(Cache<BeanKey<I>, BeanEntry<I>> cache, Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> filter, Scheduler<I, ImmutableBeanEntry<I>> scheduler, Locality oldLocality,Locality newLocality) {
            this.cache = cache;
            this.filter = filter;
            this.scheduler = scheduler;
            this.oldLocality = oldLocality;
            this.newLocality = newLocality;
        }

        @Override
        public void run() {
            // Iterate over beans in memory
            try (Stream<Map.Entry<BeanKey<I>, BeanEntry<I>>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).entrySet().stream().filter(this.filter)) {
                Iterator<Map.Entry<BeanKey<I>, BeanEntry<I>>> entries = stream.iterator();
                while (entries.hasNext()) {
                    if (Thread.currentThread().isInterrupted()) break;
                    Map.Entry<BeanKey<I>, BeanEntry<I>> entry = entries.next();
                    BeanKey<I> key = entry.getKey();
                    // If we are the new primary owner of this bean then schedule expiration of this bean locally
                    if (!this.oldLocality.isLocal(key) && this.newLocality.isLocal(key)) {
                        this.scheduler.schedule(key.getId(), entry.getValue());
                    }
                }
            }
        }
    }

    private static class SchedulableBean<I, T> implements Bean<I, T> {

        private final Bean<I, T> bean;
        private final ImmutableBeanEntry<I> entry;
        private org.wildfly.clustering.ee.Scheduler<I, ImmutableBeanEntry<I>> scheduler;

        SchedulableBean(Bean<I, T> bean, ImmutableBeanEntry<I> entry, org.wildfly.clustering.ee.Scheduler<I, ImmutableBeanEntry<I>> scheduler) {
            this.bean = bean;
            this.entry = entry;
            this.scheduler = scheduler;
        }

        @Override
        public I getId() {
            return this.bean.getId();
        }

        @Override
        public I getGroupId() {
            return this.bean.getGroupId();
        }

        @Override
        public void remove(RemoveListener<T> listener) {
            this.bean.remove(listener);
        }

        @Override
        public boolean isExpired() {
            return this.bean.isExpired();
        }

        @Override
        public boolean isValid() {
            return this.bean.isValid();
        }

        @Override
        public T acquire() {
            return this.bean.acquire();
        }

        @Override
        public boolean release() {
            return this.bean.release();
        }

        @Override
        public void close() {
            this.bean.close();
            if (this.scheduler != null) {
                if (this.bean.isValid()) {
                    this.scheduler.schedule(this.bean.getId(), this.entry);
                }
            }
        }
    }
}
