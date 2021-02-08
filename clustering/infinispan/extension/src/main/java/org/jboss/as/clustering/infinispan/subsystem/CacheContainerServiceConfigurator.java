/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute.ALIASES;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.CONTAINER;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.util.concurrent.CompletableFutures;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ServiceValueRegistry;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
@Listener
public class CacheContainerServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Function<EmbeddedCacheManager, CacheContainer>, Supplier<EmbeddedCacheManager>, Consumer<EmbeddedCacheManager> {

    private final ServiceValueRegistry<Cache<?, ?>> registry;
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();
    private final PathAddress address;
    private final String name;
    private final SupplierDependency<GlobalConfiguration> configuration;

    private volatile Registrar<String> registrar;
    private volatile ServiceName[] names;

    public CacheContainerServiceConfigurator(PathAddress address, ServiceValueRegistry<Cache<?, ?>> registry) {
        super(CONTAINER, address);
        this.address = address;
        this.name = address.getLastElement().getValue();
        this.configuration = new ServiceSupplierDependency<>(CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(address));
        this.registry = registry;
    }

    @Override
    public CacheContainer apply(EmbeddedCacheManager manager) {
        PathAddress containerAddress = this.address;
        Function<String, ServiceName> serviceNameFactory = new Function<String, ServiceName>() {
            @Override
            public ServiceName apply(String cacheName) {
                return CacheResourceDefinition.Capability.CACHE.getServiceName(containerAddress.append(CacheRuntimeResourceDefinition.pathElement(cacheName)));
            }
        };
        return new DefaultCacheContainer(manager, this.registry, serviceNameFactory);
    }

    @Override
    public EmbeddedCacheManager get() {
        GlobalConfiguration config = this.configuration.get();
        String defaultCacheName = config.defaultCacheName().orElse(null);
        ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder(config.classLoader(), new GlobalConfigurationBuilder().read(config));
        // We need to create a dummy default configuration if cache has a default cache
        if (defaultCacheName != null) {
            holder.newConfigurationBuilder(defaultCacheName);
        }
        EmbeddedCacheManager manager = new DefaultCacheManager(holder, false);
        // Undefine the default cache, if we defined one
        if (defaultCacheName != null) {
            manager.undefineConfiguration(defaultCacheName);
        }

        manager.start();
        manager.addListener(this);
        InfinispanLogger.ROOT_LOGGER.debugf("%s cache container started", this.name);
        return manager;
    }

    @Override
    public void accept(EmbeddedCacheManager manager) {
        manager.removeListener(this);
        manager.stop();
        InfinispanLogger.ROOT_LOGGER.debugf("%s cache container stopped", this.name);
    }

    @Override
    public CacheContainerServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ModelNode> aliases = ModelNodes.optionalList(ALIASES.resolveModelAttribute(context, model)).orElse(Collections.emptyList());
        this.names = new ServiceName[aliases.size() + 1];
        this.names[0] = this.getServiceName();
        for (int i = 0; i < aliases.size(); ++i) {
            this.names[i + 1] = InfinispanRequirement.CONTAINER.getServiceName(context.getCapabilityServiceSupport(), aliases.get(i).asString());
        }
        this.registrar = (CacheContainerResource) context.readResource(PathAddress.EMPTY_ADDRESS);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<CacheContainer> container = new CompositeDependency(this.configuration).register(builder).provides(this.names);
        Service service = new FunctionalService<>(container, this, this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @CacheStarted
    public CompletionStage<Void> cacheStarted(CacheStartedEvent event) {
        String cacheName = event.getCacheName();
        InfinispanLogger.ROOT_LOGGER.cacheStarted(cacheName, this.name);
        this.registrations.put(cacheName, this.registrar.register(cacheName));
        return CompletableFutures.completedNull();
    }

    @CacheStopped
    public CompletionStage<Void> cacheStopped(CacheStoppedEvent event) {
        String cacheName = event.getCacheName();
        try (Registration registration = this.registrations.remove(cacheName)) {
            InfinispanLogger.ROOT_LOGGER.cacheStopped(cacheName, this.name);
        }
        return CompletableFutures.completedNull();
    }
}
