/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Installs a service providing a dynamically created remote-cache with a custom near-cache factory with auto-managed lifecycle.
 * @author Paul Ferraro
 * @param K cache key
 * @param V cache value
 */
public class RemoteCacheServiceConfigurator<K, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<RemoteCache<K, V>>, Consumer<RemoteCache<K, V>> {

    private final String containerName;
    private final String cacheName;
    private final Consumer<RemoteCacheConfigurationBuilder> configurator;

    private volatile SupplierDependency<RemoteCacheContainer> container;

    public RemoteCacheServiceConfigurator(ServiceName name, String containerName, String cacheName, Consumer<RemoteCacheConfigurationBuilder> configurator) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.configurator = configurator;
    }

    @Override
    public RemoteCache<K, V> get() {
        RemoteCacheContainer container = this.container.get();
        container.getConfiguration().addRemoteCache(this.cacheName, this.configurator);
        RemoteCache<K, V> cache = container.getCache(this.cacheName);
        cache.start();
        return cache;
    }

    @Override
    public void accept(RemoteCache<K, V> cache) {
        cache.stop();
        this.container.get().getConfiguration().removeRemoteCache(this.cacheName);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.container = new ServiceSupplierDependency<>(InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(support, this.containerName));
        return this;
    }

    @Override
    public final ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<RemoteCache<K, V>> cache = this.container.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(cache, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
