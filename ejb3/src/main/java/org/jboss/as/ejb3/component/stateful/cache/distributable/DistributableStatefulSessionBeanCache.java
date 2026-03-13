/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import jakarta.ejb.ConcurrentAccessTimeoutException;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.Affinity;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.server.service.DecoratedService;

/**
 * A distributable stateful session bean cache.
 * The availability of bean instances managed by this cache is determined by the underlying bean manager implementation.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> extends DecoratedService implements StatefulSessionBeanCache<K, V> {
    private static final Object UNSET = Boolean.TRUE;

    private final BeanManager<K, V> manager;
    private final StatefulSessionBeanInstanceFactory<V> factory;

    public DistributableStatefulSessionBeanCache(DistributableStatefulSessionBeanCacheConfiguration<K, V> configuration) {
        super(configuration.getBeanManager());
        this.manager = configuration.getBeanManager();
        this.factory = configuration.getInstanceFactory();
    }

    @Override
    public void close() {
        this.manager.close();
    }

    @Override
    public Affinity getStrongAffinity() {
        return this.manager.getStrongAffinity();
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return this.manager.getWeakAffinity(id);
    }

    @Override
    public StatefulSessionBean<K, V> createStatefulSessionBean() {
        boolean newGroup = CURRENT_GROUP.get() == null;
        if (newGroup) {
            CURRENT_GROUP.set(UNSET);
        }
        try {
            Map.Entry<V, SuspendedBatch> entry = this.createInstance();
            // This will invoke createStatefulBean() for nested beans
            // Nested beans will share the same group identifier
            V instance = entry.getKey();
            SuspendedBatch batch = entry.getValue();
            K id = instance.getId();
            if (CURRENT_GROUP.get() == UNSET) {
                CURRENT_GROUP.set(id);
            }
            try (Context<Batch> context = batch.resumeWithContext()) {
                @SuppressWarnings("unchecked")
                Bean<K, V> bean = this.manager.createBean(instance, (K) CURRENT_GROUP.get());
                return new DistributableStatefulSessionBean<>(bean, batch);
            } catch (RuntimeException | Error e) {
                try (Context<Batch> context = batch.resumeWithContext()) {
                    rollback(context);
                }
                throw e;
            }
        } finally {
            if (newGroup) {
                CURRENT_GROUP.remove();
            }
        }
    }

    private Map.Entry<V, SuspendedBatch> createInstance() {
        SuspendedBatch batch = this.manager.getBatchFactory().get().suspend();
        try (Context<Batch> context = batch.resumeWithContext()) {
            // This will invoke createStatefulBean() for nested beans
            // Nested beans will share the same group identifier and batch context
            return Map.entry(this.factory.createInstance(), batch);
        }
    }

    @Override
    public StatefulSessionBean<K, V> findStatefulSessionBean(K id) {
        // Batch is not closed here - it will be closed by the StatefulSessionBean
        SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
        try (Context<Batch> context = suspended.resumeWithContext()) {
            // TODO WFLY-14167 Cache lookup timeout should reflect @AccessTimeout of associated bean/invocation
            Bean<K, V> bean = this.manager.findBean(id);
            return (bean != null) ? new DistributableStatefulSessionBean<>(bean, suspended) : this.close(context);
        } catch (TimeoutException e) {
            throw new ConcurrentAccessTimeoutException(e.getMessage());
        } catch (RuntimeException | Error e) {
            try (Context<Batch> batch = suspended.resumeWithContext()) {
                rollback(batch);
            }
            throw e;
        }
    }

    private StatefulSessionBean<K, V> close(Supplier<Batch> batch) {
        close(batch, Consumer.of());
        return null;
    }

    private static void rollback(Supplier<Batch> batch) {
        close(batch, Batch::discard);
    }

    private static void close(Supplier<Batch> batchProvider, Consumer<Batch> action) {
        try (Batch batch = batchProvider.get()) {
            action.accept(batch);
        } catch (RuntimeException | Error e) {
            EjbLogger.EJB3_INVOCATION_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public int getActiveCount() {
        return this.manager.getActiveCount();
    }

    @Override
    public int getPassiveCount() {
        return this.manager.getPassiveCount();
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.manager.getIdentifierFactory();
    }

    @Override
    public boolean isRemotable(Throwable throwable) {
        return this.manager.isRemotable(throwable);
    }
}
