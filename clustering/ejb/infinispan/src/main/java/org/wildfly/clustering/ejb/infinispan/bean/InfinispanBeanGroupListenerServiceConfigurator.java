/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Cache listener for bean group activation/passivation events.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public class InfinispanBeanGroupListenerServiceConfigurator<K, V extends BeanInstance<K>, C> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Service, InfinispanConfiguration {

    private final SupplierDependency<Cache<?, ?>> cache;
    private final SupplierDependency<C> context;

    private volatile Executor executor;
    private volatile ListenerRegistration postActivateListenerRegistration;
    private volatile ListenerRegistration prePassivateListenerRegistration;

    InfinispanBeanGroupListenerServiceConfigurator(ServiceName name, SupplierDependency<Cache<?, ?>> cache, SupplierDependency<C> context) {
        super(name);
        this.cache = cache;
        this.context = context;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        new CompositeDependency(this.context, this.cache).register(builder);
        return builder.setInstance(this);
    }

    @Override
    public void start(StartContext context) throws StartException {
        // We only need to listen for activation/passivation events for non-persistent caches
        // pre-passivate/post-activate callbacks for persistent caches are triggered via GroupManager
        if (!this.getCacheProperties().isPersistent()) {
            this.executor = this.getBlockingManager().asExecutor(this.getClass().getName());
            this.postActivateListenerRegistration = new PostActivateBlockingListener<>(this.getCache(), this::postActivate).register(BeanGroupKey.class);
            this.prePassivateListenerRegistration = new PrePassivateBlockingListener<>(this.getCache(), this::prePassivate).register(BeanGroupKey.class);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.executor != null) {
            this.prePassivateListenerRegistration.close();
            this.postActivateListenerRegistration.close();
        }
    }

    void postActivate(BeanGroupKey<K> key, MarshalledValue<Map<K, V>, C> value) {
        C context = this.context.get();
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Received post-activate event for bean group %s", key.getId());
        try {
            Map<K, V> instances = value.get(context);
            for (V instance : instances.values()) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking post-activate callback for bean %s", instance.getId());
                instance.postActivate();
            }
        } catch (IOException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    void prePassivate(BeanGroupKey<K> key, MarshalledValue<Map<K, V>, C> value) {
        C context = this.context.get();
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Received pre-passivate event for bean group %s", key.getId());
        @SuppressWarnings("unchecked")
        Cache<Key<K>, ?> cache = (Cache<Key<K>, ?>) this.cache.get();
        try {
            Map<K, V> instances = value.get(context);
            List<V> passivated = new ArrayList<>(instances.size());
            try {
                for (V instance : instances.values()) {
                    K id = instance.getId();
                    InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking pre-passivate callback for bean %s", id);
                    instance.prePassivate();
                    passivated.add(instance);
                    // Cascade eviction to creation meta data entry
                    this.executor.execute(() -> cache.evict(new InfinispanBeanMetaDataKey<>(id)));
                }
            } catch (RuntimeException | Error e) {
                // Restore state of pre-passivated beans
                for (V instance : passivated) {
                    InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking post-activate callback for bean %s", instance.getId());
                    try {
                        instance.postActivate();
                    } catch (RuntimeException | Error t) {
                        InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
                // Abort passivation if any beans failed to pre-passivate
                throw e;
            }
        } catch (IOException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <KK, VV> Cache<KK, VV> getCache() {
        return (Cache<KK, VV>) this.cache.get();
    }
}
