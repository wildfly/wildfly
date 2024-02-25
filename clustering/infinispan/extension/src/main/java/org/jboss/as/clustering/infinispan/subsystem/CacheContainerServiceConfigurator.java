/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.CONTAINER;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute.ALIASES;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.manager.DefaultCacheContainer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
@Listener
public class CacheContainerServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, UnaryOperator<EmbeddedCacheManager>, Supplier<EmbeddedCacheManager>, Consumer<EmbeddedCacheManager> {

    private final ServiceValueRegistry<Cache<?, ?>> registry;
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();
    private final PathAddress address;
    private final String name;
    private final SupplierDependency<GlobalConfiguration> configuration;
    private final SupplierDependency<ModuleLoader> loader;

    private volatile Registrar<String> registrar;
    private volatile ServiceName[] names;

    public CacheContainerServiceConfigurator(PathAddress address, ServiceValueRegistry<Cache<?, ?>> registry) {
        super(CONTAINER, address);
        this.address = address;
        this.name = address.getLastElement().getValue();
        this.configuration = new ServiceSupplierDependency<>(CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(address));
        this.loader = new ServiceSupplierDependency<>(Services.JBOSS_SERVICE_MODULE_LOADER);
        this.registry = registry;
    }

    @Override
    public EmbeddedCacheManager apply(EmbeddedCacheManager manager) {
        return new DefaultCacheContainer(manager, this.loader.get());
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
        List<ModelNode> aliases = ALIASES.resolveModelAttribute(context, model).asListOrEmpty();
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
        Consumer<EmbeddedCacheManager> container = new CompositeDependency(this.configuration, this.loader).register(builder).provides(this.names);
        Service service = new FunctionalService<>(container, this, this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    private ServiceName createCacheServiceName(String cacheName) {
        return CacheResourceDefinition.Capability.CACHE.getServiceName(this.address.append(CacheRuntimeResourceDefinition.pathElement(cacheName)));
    }

    @CacheStarted
    public CompletionStage<Void> cacheStarted(CacheStartedEvent event) {
        String cacheName = event.getCacheName();
        InfinispanLogger.ROOT_LOGGER.cacheStarted(cacheName, this.name);
        this.registrations.put(cacheName, this.registrar.register(cacheName));
        Consumer<Cache<?, ?>> captor = this.registry.add(ServiceDependency.on(this.createCacheServiceName(cacheName)));
        EmbeddedCacheManager container = event.getCacheManager();
        // Use getCacheAsync(), once available
        @SuppressWarnings("deprecation")
        BlockingManager blocking = container.getGlobalComponentRegistry().getComponent(BlockingManager.class);
        blocking.asExecutor(event.getCacheName()).execute(() -> captor.accept(container.getCache(cacheName)));
        return CompletableFuture.completedStage(null);
    }

    @CacheStopped
    public CompletionStage<Void> cacheStopped(CacheStoppedEvent event) {
        String cacheName = event.getCacheName();
        this.registry.remove(ServiceDependency.on(this.createCacheServiceName(cacheName)));
        try (Registration registration = this.registrations.remove(cacheName)) {
            InfinispanLogger.ROOT_LOGGER.cacheStopped(cacheName, this.name);
        }
        return CompletableFuture.completedStage(null);
    }
}
