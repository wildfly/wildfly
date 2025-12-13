/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.CacheStreamFilter;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.ImmutableBeanMetaDataFactory;
import org.wildfly.clustering.ejb.cache.bean.MutableBean;
import org.wildfly.clustering.ejb.cache.bean.OnCloseBean;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.expiration.ExpirationMetaData;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.expiration.ScheduleExpirationCommand;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactoryService;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntriesTask;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntrySchedulerService;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerCommand;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerService;
import org.wildfly.clustering.server.local.scheduler.LocalSchedulerService;
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.manager.IdentifierFactoryService;
import org.wildfly.clustering.server.scheduler.SchedulerService;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link BeanManager} implementation backed by an infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class InfinispanBeanManager<K, V extends BeanInstance<K>, M> implements BeanManager<K, V> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(BeanExpirationTask.class, WildFlySecurityManager.getClassLoaderPrivileged(BeanExpirationTask.class));

    private final Cache<BeanMetaDataKey<K>, M> cache;
    private final Predicate<Map.Entry<? super BeanMetaDataKey<K>, ? super M>> filter;
    private final BeanFactory<K, V, M> beanFactory;
    private final IdentifierFactoryService<K> identifierFactory;
    private final CacheContainerCommandDispatcherFactory dispatcherFactory;
    private final Consumer<Bean<K, V>> remover;
    private final Supplier<Batch> batchFactory;
    private final Function<K, CacheContainerGroupMember> primaryOwnerLocator;
    private final Affinity strongAffinity;
    private final SchedulerService<K, ExpirationMetaData> scheduler;

    private volatile UnaryOperator<Bean<K, V>> transformer;

    public InfinispanBeanManager(InfinispanBeanManagerConfiguration<K, V, M> configuration) {
        Cache<BeanMetaDataKey<K>, M> cache = configuration.getCache();
        this.beanFactory = configuration.getBeanFactory();
        this.cache = cache;
        this.batchFactory = configuration.getBatchFactory();
        this.identifierFactory = new AffinityIdentifierFactoryService<>(configuration.getIdentifierFactory(), this.cache);
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.remover = bean -> bean.remove(configuration.getExpirationListener());
        CacheContainerGroup group = this.dispatcherFactory.getGroup();
        this.primaryOwnerLocator = new UnaryGroupMemberAffinity<>(configuration.getCache(), group);
        this.strongAffinity = this.cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new ClusterAffinity(group.getName()) : new NodeAffinity(group.getLocalMember().getName());
        Predicate<Map.Entry<? super BeanMetaDataKey<K>, ? super M>> filter = new InfinispanBeanMetaDataFilter<>(configuration.getBeanName());
        this.filter = filter;

        ScheduledEntries<K, Instant> entries = group.isSingleton() ? ScheduledEntries.queued() : ScheduledEntries.sorted();
        Optional<Duration> maxIdle = configuration.getMaxIdle();
        Predicate<K> removeTask = !maxIdle.orElse(Duration.ZERO).isZero() ? new BeanExpirationTask<>(this.beanFactory, this.batchFactory, this.remover) : null;
        String schedulerName = String.join("/", configuration.getName(), configuration.getBeanName());
        @SuppressWarnings("resource")
        org.wildfly.clustering.server.scheduler.SchedulerService<K, Instant> localScheduler = (removeTask != null) ? new LocalSchedulerService<>(new LocalSchedulerService.Configuration<K>() {
            @Override
            public String getName() {
                return schedulerName;
            }

            @Override
            public ScheduledEntries<K, Instant> getScheduledEntries() {
                return entries;
            }

            @Override
            public Predicate<K> getTask() {
                return removeTask;
            }

            @Override
            public ThreadFactory getThreadFactory() {
                return THREAD_FACTORY;
            }

            @Override
            public Duration getCloseTimeout() {
                return configuration.getStopTimeout();
            }
        }) : null;
        ImmutableBeanMetaDataFactory<K, M> metaDataFactory = this.beanFactory.getMetaDataFactory();
        CacheEntrySchedulerService<K, BeanMetaDataKey<K>, M, ExpirationMetaData> cacheEntryScheduler = (localScheduler != null) ? new CacheEntrySchedulerService<>(localScheduler.compose(Function.identity(), ExpirationMetaData::getExpirationTime), metaDataFactory::createImmutableBeanMetaData) {
            @Override
            public void start() {
                super.start();
                // Schedule locally-owned entries
                CacheEntriesTask.schedule(InfinispanBeanManager.this.cache, InfinispanBeanManager.this.filter, this).accept(CacheStreamFilter.local(InfinispanBeanManager.this.cache));
            }
        } : null;
        this.scheduler = (cacheEntryScheduler != null) && !group.isSingleton() ? new PrimaryOwnerSchedulerService<>(new PrimaryOwnerSchedulerService.Configuration<K, ExpirationMetaData, Map.Entry<BeanMetaDataKey<K>, M>, Map.Entry<BeanMetaDataKey<K>, M>>() {
            @Override
            public String getName() {
                return schedulerName;
            }

            @Override
            public SchedulerService<K, ExpirationMetaData> getScheduler() {
                return cacheEntryScheduler;
            }

            @Override
            public EmbeddedCacheConfiguration getCacheConfiguration() {
                return configuration;
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return configuration.getCommandDispatcherFactory();
            }

            @Override
            public java.util.function.Consumer<CacheStreamFilter<Map.Entry<BeanMetaDataKey<K>, M>>> getCancelTask() {
                return CacheEntriesTask.cancel(cache, filter, cacheEntryScheduler);
            }

            @Override
            public java.util.function.Consumer<CacheStreamFilter<Map.Entry<BeanMetaDataKey<K>, M>>> getScheduleTask() {
                return CacheEntriesTask.schedule(cache, filter, cacheEntryScheduler);
            }

            @Override
            public Function<Map.Entry<K, ExpirationMetaData>, PrimaryOwnerCommand<K, ExpirationMetaData, Void>> getScheduleCommandFactory() {
                return ScheduleExpirationCommand::new;
            }
        }) : cacheEntryScheduler;
        // If bean has expiration configuration, perform expiration task on close
        Consumer<Bean<K, V>> closeTask = maxIdle.isPresent() ? bean -> {
            if (bean.isValid()) {
                if (this.scheduler != null) {
                    // Schedule expiration of bean
                    this.scheduler.schedule(bean.getId(), bean.getMetaData());
                } else {
                    // If timeout = 0, remove immediately
                    this.remover.accept(bean);
                }
            }
        } : null;
        this.transformer = (closeTask != null) ? bean -> new OnCloseBean<>(bean, closeTask) : UnaryOperator.identity();
    }

    @Override
    public boolean isStarted() {
        return this.identifierFactory.isStarted();
    }

    @Override
    public void start() {
        this.identifierFactory.start();

        if (this.scheduler != null) {
            this.scheduler.start();
        }
    }

    @Override
    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.stop();
        }
        this.identifierFactory.stop();
    }

    @Override
    public void close() {
        Consumer.close().accept(this.scheduler);
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
                    this.remover.accept(bean);
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
