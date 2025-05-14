/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.server.expiration.ExpirationMetaData;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.scheduler.AbstractCacheEntryScheduler;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.ImmutableBeanMetaDataFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.server.local.scheduler.LocalScheduler;
import org.wildfly.clustering.server.local.scheduler.LocalSchedulerConfiguration;
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Schedules a bean for expiration.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the metadata value type
 */
public class BeanExpirationScheduler<K, V extends BeanInstance<K>, M> extends AbstractCacheEntryScheduler<K, BeanMetaDataKey<K>, M, ExpirationMetaData> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(BeanExpirationScheduler.class, WildFlySecurityManager.getClassLoaderPrivileged(BeanExpirationScheduler.class));
    private final ImmutableBeanMetaDataFactory<K, M> factory;

    public BeanExpirationScheduler(String name, CacheContainerGroup group, Supplier<Batch> batchFactory, BeanFactory<K, V, M> factory, BeanExpirationConfiguration<K, V> expiration, Duration closeTimeout) {
        this(new LocalScheduler<>(new LocalSchedulerConfiguration<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public ScheduledEntries<K, Instant> getScheduledEntries() {
                return group.isSingleton() ? ScheduledEntries.linked() : ScheduledEntries.sorted();
            }

            @Override
            public Predicate<K> getTask() {
                return new BeanRemoveTask<>(batchFactory, factory, expiration.getExpirationListener());
            }

            @Override
            public ThreadFactory getThreadFactory() {
                return THREAD_FACTORY;
            }

            @Override
            public Duration getCloseTimeout() {
                return closeTimeout;
            }
        }), factory);
    }

    private BeanExpirationScheduler(Scheduler<K, Instant> scheduler, BeanFactory<K, V, M> factory) {
        super(scheduler.map(ExpirationMetaData::getExpirationTime));
        this.factory = factory.getMetaDataFactory();
    }

    @Override
    public void schedule(K id) {
        M value = this.factory.findValue(id);
        if (value != null) {
            this.schedule(Map.entry(new InfinispanBeanMetaDataKey<>(id), value));
        }
    }

    @Override
    public void schedule(Map.Entry<BeanMetaDataKey<K>, M> entry) {
        K id = entry.getKey().getId();
        ImmutableBeanMetaData<K> metaData = this.factory.createImmutableBeanMetaData(id, entry.getValue());
        this.schedule(id, metaData);
    }

    private static class BeanRemoveTask<K, V extends BeanInstance<K>, M> implements Predicate<K> {
        private final Supplier<Batch> batchFactory;
        private final BeanFactory<K, V, M> factory;
        private final Consumer<V> timeoutListener;

        BeanRemoveTask(Supplier<Batch> batchFactory, BeanFactory<K, V, M> factory, Consumer<V> timeoutListener) {
            this.batchFactory = batchFactory;
            this.timeoutListener = timeoutListener;
            this.factory = factory;
        }

        @Override
        public boolean test(K id) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", id);
            try (Batch batch = this.batchFactory.get()) {
                try {
                    M value = this.factory.tryValue(id);
                    if (value != null) {
                        try (Bean<K, V> bean = this.factory.createBean(id, value)) {
                            // Ensure bean is actually expired
                            if (bean.getMetaData().isExpired()) {
                                bean.remove(this.timeoutListener);
                            }
                        }
                    }
                    return true;
                } catch (RuntimeException e) {
                    batch.discard();
                    throw e;
                }
            } catch (RuntimeException e) {
                InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, id);
                return false;
            }
        }
    }
}
