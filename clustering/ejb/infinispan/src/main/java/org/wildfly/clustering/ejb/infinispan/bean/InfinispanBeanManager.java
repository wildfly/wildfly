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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.ee.infinispan.affinity.AffinityIdentifierFactory;
import org.wildfly.clustering.ee.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleLocalEntriesTask;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.CacheEntryScheduler;
import org.wildfly.clustering.ee.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.ee.infinispan.tx.InfinispanBatcher;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.MutableBean;
import org.wildfly.clustering.ejb.cache.bean.OnCloseBean;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.distribution.Locality;
import org.wildfly.clustering.infinispan.distribution.SimpleLocality;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class InfinispanBeanManager<K, V extends BeanInstance<K>, M> implements BeanManager<K, V, TransactionBatch> {

    private final Cache<Key<K>, Object> cache;
    private final CacheProperties properties;
    private final BeanFactory<K, V, M> beanFactory;
    private final IdentifierFactory<K> identifierFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final BeanExpirationConfiguration<K, V> expiration;
    private final Batcher<TransactionBatch> batcher;
    private final Predicate<Map.Entry<? super Key<K>, ? super Object>> filter;
    private final Function<Key<K>, Node> primaryOwnerLocator;
    private final Affinity strongAffinity;

    private volatile Scheduler<K, ExpirationMetaData> scheduler;
    private volatile ListenerRegistration schedulerListenerRegistration;
    private volatile UnaryOperator<Bean<K, V>> transformer;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<K, V, M> configuration) {
        this.beanFactory = configuration.getBeanFactory();
        this.cache = configuration.getCache();
        this.properties = configuration.getCacheProperties();
        this.batcher = new InfinispanBatcher(this.cache);
        this.identifierFactory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.cache, configuration.getAffinityFactory());
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpiration();
        this.primaryOwnerLocator = new PrimaryOwnerLocator<>(configuration.getCache(), configuration.getGroup());
        Group group = configuration.getGroup();
        this.strongAffinity = this.cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(group.getLocalMember().getName());
        this.filter = new InfinispanBeanCreationMetaDataFilter<>(configuration.getBeanName());
    }

    @Override
    public void start() {
        this.identifierFactory.start();

        Duration stopTimeout = Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout());
        CacheEntryScheduler<K, ExpirationMetaData> localScheduler = (this.expiration != null) && !this.expiration.getTimeout().isZero() ? new BeanExpirationScheduler<>(this.dispatcherFactory.getGroup(), this.batcher, this.beanFactory, this.expiration, stopTimeout) : null;

        String dispatcherName = String.join("/", this.cache.getName(), this.filter.toString());
        this.scheduler = (localScheduler != null) ? (this.dispatcherFactory.getGroup().isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(this.dispatcherFactory, dispatcherName, localScheduler, this.primaryOwnerLocator, InfinispanBeanCreationMetaDataKey::new, this.properties.isTransactional() ? ScheduleWithMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new)) : null;

        BiConsumer<Locality, Locality> scheduleTask = (localScheduler != null) ? new ScheduleLocalEntriesTask<>(this.cache, this.filter, localScheduler) : null;
        this.schedulerListenerRegistration = (localScheduler != null) ? new SchedulerTopologyChangeListener<>(this.cache, localScheduler, scheduleTask).register() : null;
        if (scheduleTask != null) {
            // Schedule expiration of existing beans that we own
            scheduleTask.accept(new SimpleLocality(false), new CacheLocality(this.cache));
        }
        // If bean has expiration configuration, perform expiration task on close
        Consumer<Bean<K, V>> closeTask = (this.expiration != null) ? bean -> {
            if (bean.isValid()) {
                if (this.scheduler != null) {
                    // Schedule expiration of bean
                    this.scheduler.schedule(bean.getId(), bean.getMetaData());
                } else {
                    // If timeout = 0, remove immediately
                    bean.remove(this.expiration.getExpirationListener());
                }
            }
        } : null;
        this.transformer = (closeTask != null) ? bean -> new OnCloseBean<>(bean, closeTask) : UnaryOperator.identity();
    }

    @Override
    public void stop() {
        if (this.schedulerListenerRegistration != null) {
            this.schedulerListenerRegistration.close();
        }
        if (this.scheduler != null) {
            this.scheduler.close();
        }
        this.identifierFactory.stop();
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
    public Affinity getStrongAffinity() {
        return this.strongAffinity;
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        org.infinispan.configuration.cache.Configuration config = this.cache.getCacheConfiguration();
        CacheMode mode = config.clustering().cacheMode();
        if (mode.isClustered()) {
            Node member = this.primaryOwnerLocator.apply(new GroupedKey<>(id));
            return new NodeAffinity(member.getName());
        }
        return Affinity.NONE;
    }

    @Override
    public Bean<K, V> createBean(V instance, K groupId) {
        K id = instance.getId();
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        MutableBean<K, V> bean = this.beanFactory.createBean(id, this.beanFactory.createValue(instance, groupId));
        bean.setInstance(instance);
        return bean;
    }

    @Override
    public Bean<K, V> findBean(K id) throws TimeoutException {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Locating bean %s", id);
        M value = this.beanFactory.findValue(id);
        if (value == null) {
            InfinispanEjbLogger.ROOT_LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        if (this.scheduler != null) {
            this.scheduler.cancel(id);
        }
        try {
            @SuppressWarnings("resource")
            Bean<K, V> bean = this.beanFactory.createBean(id, value);
            if (bean.getInstance() == null) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Bean %s metadata was found, but bean instance was not, most likely due to passivation failure.", id);
                try {
                    this.beanFactory.purge(id);
                } finally {
                    bean.close();
                }
                return null;
            }
            if (bean.getMetaData().isExpired()) {
                InfinispanEjbLogger.ROOT_LOGGER.debugf("Bean %s found, but was expired", id);
                try {
                    bean.remove(this.expiration.getExpirationListener());
                } finally {
                    bean.close();
                }
                return null;
            }
            return this.transformer.apply(bean);
        } catch (org.infinispan.util.concurrent.TimeoutException e) {
            throw new TimeoutException(e.getLocalizedMessage());
        }
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public int getActiveCount() {
        return this.count(EnumSet.of(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD));
    }

    @Override
    public int getPassiveCount() {
        return this.count(EnumSet.of(Flag.CACHE_MODE_LOCAL)) - this.getActiveCount();
    }

    private int count(Set<Flag> flags) {
        try (Stream<Key<K>> keys = this.cache.getAdvancedCache().withFlags(flags).keySet().stream()) {
            return (int) keys.filter(InfinispanBeanGroupKey.class::isInstance).count();
        }
    }
}
