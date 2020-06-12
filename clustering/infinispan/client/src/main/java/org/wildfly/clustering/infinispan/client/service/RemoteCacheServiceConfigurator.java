/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.client.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.client.InfinispanClientRequirement;
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
    private final String configurationName;
    private final Function<ClientListenerNotifier, NearCacheService<K, V>> nearCacheFactory;

    private volatile SupplierDependency<RemoteCacheContainer> container;

    public RemoteCacheServiceConfigurator(ServiceName name, String containerName, String cacheName, String configurationName) {
        this(name, containerName, cacheName, configurationName, null);
    }

    public RemoteCacheServiceConfigurator(ServiceName name, String containerName, String cacheName, String configurationName, Function<ClientListenerNotifier, NearCacheService<K, V>> nearCacheFactory) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.configurationName = configurationName;
        this.nearCacheFactory = nearCacheFactory;
    }

    @Override
    public RemoteCache<K, V> get() {
        String templateName = (this.configurationName != null) ? this.configurationName : DefaultTemplate.DIST_SYNC.getTemplateName();
        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<RemoteCacheConfigurationBuilder>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).nearCacheMode(NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE).templateName(templateName);
            }
        };
        this.container.get().getConfiguration().addRemoteCache(this.cacheName, configurator);
        RemoteCacheContainer container = this.container.get();
        try (RemoteCacheContainer.NearCacheRegistration registration = (this.nearCacheFactory != null) ? container.registerNearCacheFactory(this.cacheName, this.nearCacheFactory) : null) {
            RemoteCache<K, V> cache = container.getCache(this.cacheName);
            cache.start();
            return cache;
        }
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
