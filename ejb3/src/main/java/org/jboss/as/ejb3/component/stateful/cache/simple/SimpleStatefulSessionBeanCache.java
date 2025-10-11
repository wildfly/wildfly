/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.server.local.scheduler.LocalSchedulerService;
import org.wildfly.clustering.server.local.scheduler.ScheduledEntries;
import org.wildfly.clustering.server.scheduler.SchedulerService;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A simple stateful session bean cache implementation.
 * Bean instances are stored in memory and are lost on undeploy, shutdown, or server crash.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> implements StatefulSessionBeanCache<K, V>, Predicate<K>, Consumer<StatefulSessionBean<K, V>> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(SimpleStatefulSessionBeanCache.class, WildFlySecurityManager.getClassLoaderPrivileged(SimpleStatefulSessionBeanCache.class));

    private final Map<K, V> instances = new ConcurrentHashMap<>();
    private final Consumer<K> remover = this.instances::remove;
    private final StatefulSessionBeanInstanceFactory<V> factory;
    private final Supplier<K> identifierFactory;
    private final Duration timeout;
    private final Affinity strongAffinity;
    private final SchedulerService<K, Instant> scheduler;

    public SimpleStatefulSessionBeanCache(SimpleStatefulSessionBeanCacheConfiguration<K, V> configuration) {
        this.factory = configuration.getInstanceFactory();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.timeout = configuration.getTimeout();
        this.strongAffinity = new NodeAffinity(configuration.getEnvironment().getNodeName());
        this.scheduler = (this.timeout != null) && !this.timeout.isZero() && !this.timeout.isNegative() ? new LocalSchedulerService<>(new LocalSchedulerService.Configuration<>() {
            @Override
            public String getName() {
                return configuration.getComponentName();
            }

            @Override
            public ScheduledEntries<K, Instant> getScheduledEntries() {
                return ScheduledEntries.queued();
            }

            @Override
            public Predicate<K> getTask() {
                return SimpleStatefulSessionBeanCache.this;
            }

            @Override
            public ThreadFactory getThreadFactory() {
                return THREAD_FACTORY;
            }
        }) : null;
    }

    @Override
    public boolean isStarted() {
        return (this.scheduler != null) ? this.scheduler.isStarted() : true;
    }

    @Override
    public void start() {
        if (this.scheduler != null) {
            this.scheduler.start();
        }
    }

    @Override
    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.stop();
        }
        for (V instance : this.instances.values()) {
            instance.removed();
        }
        this.instances.clear();
    }

    @Override
    public void close() {
        if (this.scheduler != null) {
            this.scheduler.close();
        }
    }

    @Override
    public Affinity getStrongAffinity() {
        return this.strongAffinity;
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return Affinity.NONE;
    }

    @Override
    public void accept(StatefulSessionBean<K, V> bean) {
        if (this.timeout != null) {
            K id = bean.getId();
            if (this.scheduler != null) {
                // Timeout > 0, schedule bean to expire
                this.scheduler.schedule(id, Instant.now().plus(this.timeout));
            } else {
                // Timeout = 0, remove bean immediately
                this.test(id);
            }
        }
    }

    @Override
    public boolean test(K id) {
        V instance = this.instances.remove(id);
        if (instance != null) {
            instance.removed();
        }
        return true;
    }

    @Override
    public StatefulSessionBean<K, V> createStatefulSessionBean() {
        if (CURRENT_GROUP.get() != null) {
            // An SFSB that uses a distributable cache cannot contain an SFSB that uses a simple cache
            throw EjbLogger.ROOT_LOGGER.incompatibleCaches();
        }
        V instance = this.factory.createInstance();
        K id = instance.getId();
        this.instances.put(id, instance);
        return new SimpleStatefulSessionBean<>(instance, this.remover, this);
    }

    @Override
    public StatefulSessionBean<K, V> findStatefulSessionBean(K id) {
        V instance = this.instances.get(id);
        if (instance == null) return null;
        if (this.scheduler != null) {
            this.scheduler.cancel(id);
        }
        return new SimpleStatefulSessionBean<>(instance, this.remover, this);
    }

    @Override
    public int getActiveCount() {
        return this.instances.size();
    }

    @Override
    public int getPassiveCount() {
        return 0;
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.identifierFactory;
    }
}
