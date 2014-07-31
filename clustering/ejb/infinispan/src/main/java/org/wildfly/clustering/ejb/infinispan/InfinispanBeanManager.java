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
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.KeyFilter;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.distribution.ConsistentHashLocality;
import org.jboss.as.clustering.infinispan.distribution.Locality;
import org.jboss.as.clustering.infinispan.distribution.SimpleLocality;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;

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
public class InfinispanBeanManager<G, I, T> implements BeanManager<G, I, T, TransactionBatch>, KeyFilter {

    private final Cache<G, BeanGroupEntry<I, T>> groupCache;
    private final String beanName;
    private final Cache<BeanKey<I>, BeanEntry<G>> beanCache;
    private final BeanFactory<G, I, T> beanFactory;
    private final BeanGroupFactory<G, I, T> groupFactory;
    private final IdentifierFactory<G> groupIdentifierFactory;
    private final IdentifierFactory<I> beanIdentifierFactory;
    private final List<KeyAffinityService<?>> affinityServices = new ArrayList<>(2);
    private final Registry<String, ?> registry;
    private final NodeFactory<Address> nodeFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final ExpirationConfiguration<T> expiration;
    private final PassivationConfiguration<T> passivation;
    private final AtomicInteger passiveCount = new AtomicInteger();
    private final Batcher<TransactionBatch> batcher;
    private volatile Scheduler<I> scheduler;
    private volatile CommandDispatcher<Scheduler<I>> dispatcher;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<T> configuration, final Configuration<I, BeanKey<I>, BeanEntry<G>, BeanFactory<G, I, T>> beanConfiguration, final Configuration<G, G, BeanGroupEntry<I, T>, BeanGroupFactory<G, I, T>> groupConfiguration) {
        this.beanName = configuration.getBeanName();
        this.groupFactory = groupConfiguration.getFactory();
        this.beanFactory = beanConfiguration.getFactory();
        this.groupCache = groupConfiguration.getCache();
        this.beanCache = beanConfiguration.getCache();
        this.batcher = new InfinispanBatcher(this.groupCache);
        final Address address = this.groupCache.getCacheManager().getAddress();
        final KeyGenerator<G> groupKeyGenerator = new KeyGenerator<G>() {
            @Override
            public G getKey() {
                return groupConfiguration.getIdentifierFactory().createIdentifier();
            }
        };
        KeyAffinityServiceFactory affinityFactory = configuration.getAffinityFactory();
        final KeyAffinityService<G> groupAffinity = affinityFactory.createService(this.groupCache, groupKeyGenerator);
        this.groupIdentifierFactory = new IdentifierFactory<G>() {
            @Override
            public G createIdentifier() {
                return groupAffinity.getKeyForAddress(address);
            }
        };
        this.affinityServices.add(groupAffinity);
        final KeyGenerator<BeanKey<I>> beanKeyGenerator = new KeyGenerator<BeanKey<I>>() {
            @Override
            public BeanKey<I> getKey() {
                return beanConfiguration.getFactory().createKey(beanConfiguration.getIdentifierFactory().createIdentifier());
            }
        };
        final KeyAffinityService<BeanKey<I>> beanAffinity = affinityFactory.createService(this.beanCache, beanKeyGenerator);
        this.beanIdentifierFactory = new IdentifierFactory<I>() {
            @Override
            public I createIdentifier() {
                return beanAffinity.getKeyForAddress(address).getId();
            }
        };
        this.affinityServices.add(beanAffinity);
        this.registry = configuration.getRegistry();
        this.nodeFactory = configuration.getNodeFactory();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpirationConfiguration();
        this.passivation = configuration.getPassivationConfiguration();
    }

    @Override
    public void start() {
        for (KeyAffinityService<?> service: this.affinityServices) {
            service.start();
        }
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
                for (Scheduler<I> scheduler: schedulers) {
                    scheduler.schedule(id);
                }
            }

            @Override
            public void cancel(I id) {
                for (Scheduler<I> scheduler: schedulers) {
                    scheduler.cancel(id);
                }
            }

            @Override
            public void cancel(Locality locality) {
                for (Scheduler<I> scheduler: schedulers) {
                    scheduler.cancel(locality);
                }
            }

            @Override
            public void close() {
                for (Scheduler<I> scheduler: schedulers) {
                    scheduler.close();
                }
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.beanName + ".schedulers", this.scheduler);
        this.beanCache.addListener(this, this);
        this.schedule(this.beanCache, new SimpleLocality(false), new ConsistentHashLocality(this.beanCache));
    }

    @Override
    public void stop() {
        this.beanCache.removeListener(this);
        this.dispatcher.close();
        this.scheduler.close();
        for (KeyAffinityService<?> service: this.affinityServices) {
            service.stop();
        }
    }

    @Override
    public boolean accept(Object key) {
        if (!(key instanceof BeanKey)) return false;
        @SuppressWarnings("unchecked")
        BeanKey<I> beanKey = (BeanKey<I>) key;
        return beanKey.getBeanName().equals(this.beanName);
    }

    @Override
    public Affinity getStrictAffinity() {
        return this.beanCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(this.registry.getGroup().getName()) : new NodeAffinity(this.registry.getLocalEntry().getKey());
    }

    @Override
    public Affinity getWeakAffinity(I id) {
        return this.beanCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new NodeAffinity(this.registry.getEntry(this.locatePrimaryOwner(id)).getKey()) : Affinity.NONE;
    }

    void cancel(Bean<G, I, T> bean) {
        // This should only go remote following a failover
        this.dispatcher.executeOnNode(new CancelSchedulerCommand<>(bean.getId()), this.locatePrimaryOwner(bean.getId()));
    }

    void schedule(Bean<G, I, T> bean) {
        // This should only go remote following a failover
        this.dispatcher.executeOnNode(new ScheduleSchedulerCommand<>(bean.getId()), this.locatePrimaryOwner(bean.getId()));
    }

    private Node locatePrimaryOwner(I id) {
        DistributionManager dist = this.beanCache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getPrimaryLocation(id) : null;
        return (address != null) ? this.nodeFactory.createNode(address) : this.registry.getGroup().getLocalNode();
    }

    @Override
    public Bean<G, I, T> createBean(I id, G groupId, T bean) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        BeanGroup<G, I, T> group = this.groupFactory.createGroup(groupId, this.groupFactory.createValue(groupId));
        group.addBean(id, bean);
        group.releaseBean(id, this.passivation.isPersistent() ? this.passivation.getPassivationListener() : null);
        return new SchedulableBean(this.beanFactory.createBean(id, this.beanFactory.createValue(id, groupId)));
    }

    @Override
    public Bean<G, I, T> findBean(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Locating bean %s", id);
        BeanEntry<G> entry = this.beanFactory.findValue(id);
        if (entry == null) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        Bean<G, I, T> bean = this.beanFactory.createBean(id, entry);
        this.cancel(bean);
        return new SchedulableBean(bean);
    }

    @Override
    public boolean containsBean(I id) {
        return this.beanCache.containsKey(this.beanFactory.createKey(id));
    }

    @Override
    public IdentifierFactory<G> getGroupIdentifierFactory() {
        return this.groupIdentifierFactory;
    }

    @Override
    public IdentifierFactory<I> getBeanIdentifierFactory() {
        return this.beanIdentifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public int getActiveCount() {
        int size = 0;
        for (Object key: this.beanCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet()) {
            if (this.accept(key)) {
                size += 1;
            }
        }
        return size;
    }

    @Override
    public int getPassiveCount() {
        return this.passiveCount.get();
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<BeanKey<I>, BeanEntry<G>> event) {
        if (event.isPre()) {
            this.passiveCount.incrementAndGet();
            if (!this.passivation.isPersistent()) {
                G groupId = event.getValue().getGroupId();
                BeanGroupEntry<I, T> entry = this.groupFactory.findValue(groupId);
                if (entry != null) {
                    try (BeanGroup<G, I, T> group = this.groupFactory.createGroup(groupId, entry)) {
                        group.prePassivate(event.getKey().getId(), this.passivation.getPassivationListener());
                    }
                }
            }
        }
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<BeanKey<I>, BeanEntry<G>> event) {
        if (!event.isPre()) {
            this.passiveCount.decrementAndGet();
            if (!this.passivation.isPersistent()) {
                G groupId = event.getValue().getGroupId();
                BeanGroupEntry<I, T> entry = this.groupFactory.findValue(groupId);
                if (entry != null) {
                    try (BeanGroup<G, I, T> group = this.groupFactory.createGroup(groupId, entry)) {
                        group.postActivate(event.getKey().getId(), this.passivation.getPassivationListener());
                    }
                }
            }
        }
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<BeanKey<I>, BeanEntry<G>> event) {
        Cache<BeanKey<I>, BeanEntry<G>> cache = event.getCache();
        Address localAddress = cache.getCacheManager().getAddress();
        Locality oldLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtStart());
        Locality newLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtEnd());
        if (event.isPre()) {
            this.scheduler.cancel(newLocality);
        } else {
            this.schedule(cache, oldLocality, newLocality);
        }
    }

    private void schedule(Cache<BeanKey<I>, BeanEntry<G>> cache, Locality oldLocality, Locality newLocality) {
        // Iterate over sessions in memory
        for (Object key: cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet()) {
            // Cache may contain non-string keys, so ignore any others
            if (this.accept(key)) {
                @SuppressWarnings("unchecked")
                I id = ((BeanKey<I>) key).getId();
                // If we are the new primary owner of this session
                // then schedule expiration of this session locally
                if (!oldLocality.isLocal(id) && newLocality.isLocal(id)) {
                    this.scheduler.schedule(id);
                }
            }
        }
    }

    private class SchedulableBean implements Bean<G, I, T> {

        private final Bean<G, I, T> bean;

        SchedulableBean(Bean<G, I, T> bean) {
            this.bean = bean;
        }

        @Override
        public I getId() {
            return this.bean.getId();
        }

        @Override
        public G getGroupId() {
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
