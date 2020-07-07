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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.infinispan.remoting.transport.Address;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleLocalEntriesTask;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerListener;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.ee.infinispan.tx.InfinispanBatcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
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
    private final Function<BeanKey<I>, Node> primaryOwnerLocator;

    private volatile org.wildfly.clustering.ee.Scheduler<I, ImmutableBeanEntry<I>> scheduler;
    private volatile SchedulerListener listener;

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

        Scheduler<I, ImmutableBeanEntry<I>> localScheduler = !schedulers.isEmpty() ? new CompositeScheduler<>(schedulers) : null;
        this.scheduler = (localScheduler != null) ? (this.dispatcherFactory.getGroup().isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(this.dispatcherFactory, dispatcherName, localScheduler, this.primaryOwnerLocator, InfinispanBeanKey::new)) : null;

        BiConsumer<Locality, Locality> scheduleTask = new ScheduleLocalEntriesTask<>(this.cache, this.filter, localScheduler);
        this.listener = (localScheduler != null) ? new SchedulerTopologyChangeListener<>(this.cache, localScheduler, scheduleTask) : null;
        if (this.listener != null) {
            scheduleTask.accept(new SimpleLocality(false), new CacheLocality(this.cache));
        }
    }

    @Override
    public void stop() {
        if (this.listener != null) {
            this.listener.close();
        }
        if (this.scheduler != null) {
            this.scheduler.close();
        }
        this.affinity.stop();
        this.groupFactory.close();
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
            Node node = this.primaryOwnerLocator.apply(new InfinispanBeanKey<>(id));
            Map.Entry<String, ?> entry = this.registry.getEntry(node);
            if (entry != null) {
                return new NodeAffinity(entry.getKey());
            }
        }
        return Affinity.NONE;
    }

    @Override
    public Bean<I, T> createBean(I id, I groupId, T bean) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        BeanGroupEntry<I, T, C> groupEntry = (id == groupId) ? this.groupFactory.createValue(groupId, null) : this.groupFactory.findValue(groupId);
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry);
        group.addBean(id, bean);
        group.releaseBean(id, this.properties.isPersistent() ? this.passivation.getPassivationListener() : null);
        BeanEntry<I> entry = this.beanFactory.createValue(id, groupId);
        return new SchedulableBean<>(this.beanFactory.createBean(id, entry), entry, this.scheduler);
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
        if (this.scheduler != null) {
            this.scheduler.cancel(id);
        }
        return new SchedulableBean<>(bean, entry, this.scheduler);
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
