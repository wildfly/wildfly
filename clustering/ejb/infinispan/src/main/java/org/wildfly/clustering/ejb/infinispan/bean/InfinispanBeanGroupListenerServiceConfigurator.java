/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PostPassivateBlockingListener;
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
    private volatile ListenerRegistration postPassivateListenerRegistration;

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
            this.postPassivateListenerRegistration = new PostPassivateBlockingListener<>(this.getCache(), this::cascadeEvict).register(BeanCreationMetaDataKey.class);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.executor != null) {
            this.postPassivateListenerRegistration.close();
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
                    this.executor.execute(() -> cache.evict(new InfinispanBeanCreationMetaDataKey<>(id)));
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

    void cascadeEvict(BeanCreationMetaDataKey<K> key) {
        // Cascade eviction to access meta data entry
        this.getCache().evict(new InfinispanBeanAccessMetaDataKey<>(key.getId()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <KK, VV> Cache<KK, VV> getCache() {
        return (Cache<KK, VV>) this.cache.get();
    }
}
