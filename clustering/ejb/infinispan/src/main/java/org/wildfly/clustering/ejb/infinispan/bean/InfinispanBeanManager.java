/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import io.github.resilience4j.retry.RetryConfig;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.CacheStreamFilter;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.MutableBean;
import org.wildfly.clustering.ejb.cache.bean.OnCloseBean;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.expiration.ExpirationMetaData;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.expiration.ScheduleWithExpirationMetaDataCommand;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactory;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntriesTask;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerConfiguration;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleCommand;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.server.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.server.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.server.manager.IdentifierFactory;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class InfinispanBeanManager<K, V extends BeanInstance<K>, M> implements BeanManager<K, V> {

    private final Cache<BeanMetaDataKey<K>, M> cache;
    private final CacheProperties properties;
    private final RetryConfig retryConfig;
    private final BeanFactory<K, V, M> beanFactory;
    private final IdentifierFactory<K> identifierFactory;
    private final CacheContainerCommandDispatcherFactory dispatcherFactory;
    private final BeanExpirationConfiguration<K, V> expiration;
    private final Supplier<Batch> batchFactory;
    private final Predicate<Map.Entry<? super BeanMetaDataKey<K>, ? super M>> filter;
    private final Function<K, CacheContainerGroupMember> primaryOwnerLocator;
    private final Affinity strongAffinity;

    private volatile Scheduler<K, ExpirationMetaData> scheduler;
    private volatile ListenerRegistration schedulerListenerRegistration;
    private volatile UnaryOperator<Bean<K, V>> transformer;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<K, V, M> configuration) {
        this.beanFactory = configuration.getBeanFactory();
        this.cache = configuration.getCache();
        this.properties = configuration.getCacheProperties();
        this.retryConfig = configuration.getRetryConfig();
        this.batchFactory = configuration.getBatchFactory();
        this.identifierFactory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.cache);
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.expiration = configuration.getExpiration();
        CacheContainerGroup group = this.dispatcherFactory.getGroup();
        this.primaryOwnerLocator = new UnaryGroupMemberAffinity<>(configuration.getCache(), group);
        this.strongAffinity = this.cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(group.getLocalMember().getName());
        this.filter = new InfinispanBeanMetaDataFilter<>(configuration.getBeanName());
    }

    @Override
    public boolean isStarted() {
        return this.identifierFactory.isStarted();
    }

    @SuppressWarnings("resource")
    @Override
    public void start() {
        this.identifierFactory.start();

        Duration stopTimeout = Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout());
        BeanExpirationScheduler<K, V, M> localScheduler = (this.expiration != null) && !this.expiration.getTimeout().isZero() ? new BeanExpirationScheduler<>(this.cache.getName(), this.dispatcherFactory.getGroup(), this.batchFactory, this.beanFactory, this.expiration, stopTimeout) : null;

        String dispatcherName = String.join("/", this.cache.getName(), this.filter.toString());
        this.scheduler = (localScheduler != null) ? (this.dispatcherFactory.getGroup().isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(new PrimaryOwnerSchedulerConfiguration<>() {
            @Override
            public String getName() {
                return dispatcherName;
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return InfinispanBeanManager.this.dispatcherFactory;
            }

            @Override
            public Scheduler<K, ExpirationMetaData> getScheduler() {
                return localScheduler;
            }

            @Override
            public Function<K, CacheContainerGroupMember> getAffinity() {
                return InfinispanBeanManager.this.primaryOwnerLocator;
            }

            @Override
            public BiFunction<K, ExpirationMetaData, ScheduleCommand<K, ExpirationMetaData>> getScheduleCommandFactory() {
                return InfinispanBeanManager.this.properties.isTransactional() ? ScheduleWithExpirationMetaDataCommand::new : ScheduleWithTransientMetaDataCommand::new;
            }

            @Override
            public RetryConfig getRetryConfig() {
                return InfinispanBeanManager.this.retryConfig;
            }
        })) : null;

        Consumer<CacheStreamFilter<Map.Entry<BeanMetaDataKey<K>, M>>> scheduleTask = (localScheduler != null) ? CacheEntriesTask.schedule(this.cache, this.filter, localScheduler) : null;
        Consumer<CacheStreamFilter<Map.Entry<BeanMetaDataKey<K>, M>>> cancelTask = (localScheduler != null) ? CacheEntriesTask.cancel(this.cache, this.filter, localScheduler) : null;
        this.schedulerListenerRegistration = (localScheduler != null) ? new SchedulerTopologyChangeListener<>(this.cache, scheduleTask, cancelTask).register() : null;
        if (scheduleTask != null) {
            // Schedule expiration of existing beans that we own
            scheduleTask.accept(CacheStreamFilter.local(this.cache));
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
            CacheContainerGroupMember member = this.primaryOwnerLocator.apply(id);
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
        } catch (org.infinispan.commons.TimeoutException e) {
            throw new TimeoutException(e.getLocalizedMessage());
        }
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Supplier<Batch> getBatchFactory() {
        return this.batchFactory;
    }

    @Override
    public int getActiveCount() {
        return this.count(EnumSet.of(Flag.SKIP_CACHE_LOAD));
    }

    @Override
    public int getPassiveCount() {
        return this.count(Set.of()) - this.getActiveCount();
    }

    private int count(Set<Flag> flags) {
        CacheStreamFilter<Map.Entry<BeanMetaDataKey<K>, M>> filter = CacheStreamFilter.local(this.cache);
        try (Stream<Map.Entry<BeanMetaDataKey<K>, M>> entries = filter.apply(this.cache.getAdvancedCache().withFlags(flags).entrySet().stream())) {
            return (int) entries.filter(this.filter).count();
        }
    }
}
