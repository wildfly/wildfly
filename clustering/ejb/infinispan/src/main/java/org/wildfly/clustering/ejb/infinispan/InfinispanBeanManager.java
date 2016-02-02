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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.RetryingInvoker;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
@Listener(primaryOnly = true)
public class InfinispanBeanManager<I, T> implements BeanManager<I, T, TransactionBatch> {

    private final Cache<I, BeanGroupEntry<I, T>> groupCache;
    private final String beanName;
    private final Cache<BeanKey<I>, BeanEntry<I>> beanCache;
    private final BeanFactory<I, T> beanFactory;
    private final BeanGroupFactory<I, T> groupFactory;
    private final IdentifierFactory<I> identifierFactory;
    private final KeyAffinityService<BeanKey<I>> affinity;
    private final Registry<String, ?> registry;
    private final NodeFactory<Address> nodeFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final ExpirationConfiguration<T> expiration;
    private final PassivationConfiguration<T> passivation;
    private final AtomicInteger passiveCount = new AtomicInteger();
    private final Batcher<TransactionBatch> batcher;
    private final Invoker invoker = new RetryingInvoker(0, 10, 100);
    private final BeanKeyFilter<I> filter;

    private volatile CommandDispatcher<Scheduler<I>> dispatcher;
    private volatile Scheduler<I> scheduler;
    private volatile ServiceExecutor executor;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<T> configuration, IdentifierFactory<I> identifierFactory, Configuration<BeanKey<I>, BeanEntry<I>, BeanFactory<I, T>> beanConfiguration, Configuration<I, BeanGroupEntry<I, T>, BeanGroupFactory<I, T>> groupConfiguration) {
        this.beanName = configuration.getBeanName();
        this.groupFactory = groupConfiguration.getFactory();
        this.beanFactory = beanConfiguration.getFactory();
        this.groupCache = groupConfiguration.getCache();
        this.beanCache = beanConfiguration.getCache();
        this.batcher = new InfinispanBatcher(this.groupCache);
        this.filter = new BeanKeyFilter<>(this.beanName);
        Address address = this.groupCache.getCacheManager().getAddress();
        KeyAffinityServiceFactory affinityFactory = configuration.getAffinityFactory();
        KeyGenerator<BeanKey<I>> beanKeyGenerator = () -> beanConfiguration.getFactory().createKey(identifierFactory.createIdentifier());
        this.affinity = affinityFactory.createService(this.beanCache, beanKeyGenerator);
        this.identifierFactory = () -> this.affinity.getKeyForAddress(address).getId();
        this.registry = configuration.getRegistry();
        this.nodeFactory = configuration.getNodeFactory();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpirationConfiguration();
        this.passivation = configuration.getPassivationConfiguration();
    }

    @Override
    public void start() {
        this.executor = new StampedLockServiceExecutor();
        this.affinity.start();
        final List<Scheduler<I>> schedulers = new ArrayList<>(2);
        Time timeout = this.expiration.getTimeout();
        if ((timeout != null) && (timeout.getValue() >= 0)) {
            schedulers.add(new BeanExpirationScheduler<>(this.batcher, new ExpiredBeanRemover<>(this.beanFactory), this.expiration));
        }
        if (this.passivation.isEvictionAllowed()) {
            schedulers.add(new BeanEvictionScheduler<>(this.beanName + ".eviction", this.batcher, this.beanFactory, this.dispatcherFactory, this.passivation));
        }
        this.scheduler = new Scheduler<I>() {
            @Override
            public void schedule(I id) {
                schedulers.forEach(scheduler -> scheduler.schedule(id));
            }

            @Override
            public void cancel(I id) {
                schedulers.forEach(scheduler -> scheduler.cancel(id));
            }

            @Override
            public void cancel(Locality locality) {
                schedulers.forEach(scheduler -> scheduler.cancel(locality));
            }

            @Override
            public void close() {
                schedulers.forEach(scheduler -> scheduler.close());
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.beanName + ".schedulers", this.scheduler);
        this.beanCache.addListener(this, this.filter);
        this.schedule(this.beanCache, new SimpleLocality(false), new ConsistentHashLocality(this.beanCache));
    }

    @Override
    public void stop() {
        this.executor.close(() -> {
            this.beanCache.removeListener(this);
            this.dispatcher.close();
            this.scheduler.close();
            this.affinity.stop();
        });
    }

    @Override
    public Affinity getStrictAffinity() {
        Group group = this.registry.getGroup();
        return this.beanCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(this.registry.getEntry(group.getLocalNode()).getKey());
    }

    @Override
    public Affinity getWeakAffinity(I id) {
        if (this.beanCache.getCacheConfiguration().clustering().cacheMode().isClustered()) {
            Node node = this.locatePrimaryOwner(id);
            Map.Entry<String, ?> entry = this.registry.getEntry(node);
            if (entry != null) {
                return new NodeAffinity(entry.getKey());
            }
        }
        return Affinity.NONE;
    }

    private void cancel(Bean<I, T> bean) {
        try {
            this.executeOnPrimaryOwner(bean, new CancelSchedulerCommand<>(bean.getId()));
        } catch (Exception e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToCancelBean(e, bean.getId());
        }
    }

    void schedule(Bean<I, T> bean) {
        try {
            this.executeOnPrimaryOwner(bean, new ScheduleSchedulerCommand<>(bean.getId()));
        } catch (Exception e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToScheduleBean(e, bean.getId());
        }
    }

    private void executeOnPrimaryOwner(Bean<I, T> bean, final Command<Void, Scheduler<I>> command) throws Exception {
        this.invoker.invoke(() -> {
            // This should only go remote following a failover
            Node node = InfinispanBeanManager.this.locatePrimaryOwner(bean.getId());
            return InfinispanBeanManager.this.dispatcher.executeOnNode(command, node);
        }).get();
    }

    Node locatePrimaryOwner(I id) {
        DistributionManager dist = this.beanCache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getPrimaryLocation(id) : null;
        return (address != null) ? this.nodeFactory.createNode(address) : this.registry.getGroup().getLocalNode();
    }

    @Override
    public Bean<I, T> createBean(I id, I groupId, T bean) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, this.groupFactory.createValue(groupId, null));
        group.addBean(id, bean);
        group.releaseBean(id, this.passivation.isPersistent() ? this.passivation.getPassivationListener() : null);
        return new SchedulableBean(this.beanFactory.createBean(id, this.beanFactory.createValue(id, groupId)));
    }

    @Override
    public Bean<I, T> findBean(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Locating bean %s", id);
        BeanEntry<I> entry = this.beanFactory.findValue(id);
        if (entry == null) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        Bean<I, T> bean = this.beanFactory.createBean(id, entry);
        this.cancel(bean);
        return new SchedulableBean(bean);
    }

    @Override
    public boolean containsBean(I id) {
        return this.beanCache.containsKey(this.beanFactory.createKey(id));
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
        try (Stream<BeanKey<I>> keys = this.beanCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet().stream()) {
            return (int) keys.filter(this.filter).count();
        }
    }

    @Override
    public int getPassiveCount() {
        return this.passiveCount.get();
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<BeanKey<I>, BeanEntry<I>> event) {
        if (event.isPre()) {
            this.passiveCount.incrementAndGet();
            if (!this.passivation.isPersistent()) {
                this.executor.execute(() -> {
                    I groupId = event.getValue().getGroupId();
                    BeanGroupEntry<I, T> entry = this.groupFactory.findValue(groupId);
                    if (entry != null) {
                        this.groupFactory.createGroup(groupId, entry).prePassivate(event.getKey().getId(), this.passivation.getPassivationListener());
                    }
                });
            }
        }
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<BeanKey<I>, BeanEntry<I>> event) {
        if (!event.isPre()) {
            this.passiveCount.decrementAndGet();
            if (!this.passivation.isPersistent()) {
                this.executor.execute(() -> {
                    I groupId = event.getValue().getGroupId();
                    BeanGroupEntry<I, T> entry = this.groupFactory.findValue(groupId);
                    if (entry != null) {
                        this.groupFactory.createGroup(groupId, entry).postActivate(event.getKey().getId(), this.passivation.getPassivationListener());
                    }
                });
            }
        }
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<BeanKey<I>, BeanEntry<I>> event) {
        this.executor.execute(() -> {
            Cache<BeanKey<I>, BeanEntry<I>> cache = event.getCache();
            Address localAddress = cache.getCacheManager().getAddress();
            Locality oldLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtStart());
            Locality newLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtEnd());
            if (event.isPre()) {
                this.scheduler.cancel(newLocality);
            } else {
                this.schedule(cache, oldLocality, newLocality);
            }
        });
    }

    private void schedule(Cache<BeanKey<I>, BeanEntry<I>> cache, Locality oldLocality, Locality newLocality) {
        // Iterate over sessions in memory
        try (Stream<BeanKey<I>> stream = this.beanCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet().stream()) {
            // If we are the new primary owner of this session then schedule expiration of this session locally
            stream.filter(this.filter).map(key -> key.getId()).filter(id -> !oldLocality.isLocal(id) && newLocality.isLocal(id)).forEach(id -> this.scheduler.schedule(id));
        }
    }

    private class SchedulableBean implements Bean<I, T> {

        private final Bean<I, T> bean;

        SchedulableBean(Bean<I, T> bean) {
            this.bean = bean;
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
            InfinispanBeanManager.this.schedule(this.bean);
        }
    }
}
