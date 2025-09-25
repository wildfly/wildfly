/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockConfiguration;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.infinispan.cache.LazyCache;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.manager.DefaultCacheContainer;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * Configures a service providing a cache container.
 * @author Paul Ferraro
 */
public class CacheContainerServiceConfigurator implements ResourceServiceConfigurator {
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONTAINER).build();

    private final ServiceValueRegistry<EmbeddedCacheManager> containerRegistry;
    private final ServiceValueRegistry<Cache<?, ?>> cacheRegistry;

    public CacheContainerServiceConfigurator(ServiceValueRegistry<EmbeddedCacheManager> containerRegistry, ServiceValueRegistry<Cache<?, ?>> cacheRegistry) {
        this.containerRegistry = containerRegistry;
        this.cacheRegistry = cacheRegistry;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<String> aliases = CacheContainerResourceDefinitionRegistrar.ALIASES.resolveModelAttribute(context, model).asListOrEmpty().stream().map(ModelNode::asString).toList();

        Object listener = new CacheLifecycleListener(this.cacheRegistry, (CacheContainerResource) context.readResource(PathAddress.EMPTY_ADDRESS));

        ServiceDependency<EmbeddedCacheManager> container = ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION, name).map(new Function<>() {
            @Override
            public EmbeddedCacheManager apply(GlobalConfiguration global) {
                String defaultCacheName = global.defaultCacheName().orElse(null);
                ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder(global.classLoader(), new GlobalConfigurationBuilder().read(global));
                // We need to create a dummy default configuration if cache has a default cache
                if (defaultCacheName != null) {
                    holder.newConfigurationBuilder(defaultCacheName);
                }
                EmbeddedCacheManager manager = new DefaultCacheManager(holder, false);
                // Undefine the default cache, if we defined one
                if (defaultCacheName != null) {
                    manager.undefineConfiguration(defaultCacheName);
                }
                return manager;
            }
        });
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        UnaryOperator<EmbeddedCacheManager> wrapper = new UnaryOperator<>() {
            @Override
            public EmbeddedCacheManager apply(EmbeddedCacheManager manager) {
                return new DefaultCacheContainer(manager, loader.get());
            }
        };
        ServiceValueRegistry<EmbeddedCacheManager> containerRegistry = this.containerRegistry;
        Consumer<EmbeddedCacheManager> start = new Consumer<>() {
            @Override
            public void accept(EmbeddedCacheManager manager) {
                manager.start();
                // If manager defined a ClusteredLockManager, override with privileged implementation
                ClusteredLockManager lockManager = GlobalComponentRegistry.componentOf(manager, ClusteredLockManager.class);
                if (lockManager != null) {
                    BasicComponentRegistry registry = GlobalComponentRegistry.componentOf(manager, BasicComponentRegistry.class);
                    registry.replaceComponent(ClusteredLockManager.class.getName(), new ClusteredLockManager() {
                        @Override
                        public boolean defineLock(String name) {
                            return AccessController.doPrivileged(new PrivilegedAction<>() {
                                @Override
                                public Boolean run() {
                                    return lockManager.defineLock(name);
                                }
                            });
                        }

                        @Override
                        public boolean defineLock(String name, ClusteredLockConfiguration configuration) {
                            return AccessController.doPrivileged(new PrivilegedAction<>() {
                                @Override
                                public Boolean run() {
                                    return lockManager.defineLock(name, configuration);
                                }
                            });
                        }

                        @Override
                        public ClusteredLock get(String name) {
                            return AccessController.doPrivileged(new PrivilegedAction<>() {
                                @Override
                                public ClusteredLock run() {
                                    return lockManager.get(name);
                                }
                            });
                        }

                        @Override
                        public ClusteredLockConfiguration getConfiguration(String name) {
                            return lockManager.getConfiguration(name);
                        }

                        @Override
                        public boolean isDefined(String name) {
                            return lockManager.isDefined(name);
                        }

                        @Override
                        public CompletableFuture<Boolean> remove(String name) {
                            return lockManager.remove(name);
                        }

                        @Override
                        public CompletableFuture<Boolean> forceRelease(String name) {
                            return lockManager.forceRelease(name);
                        }
                    }, false);
                    registry.rewire();
                }
                containerRegistry.add(ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, name)).accept(manager);
                manager.addListener(listener);
                InfinispanLogger.ROOT_LOGGER.infof("Started %s cache container", name);
            }
        };
        Consumer<EmbeddedCacheManager> stop = new Consumer<>() {
            @Override
            public void accept(EmbeddedCacheManager manager) {
                manager.removeListener(listener);
                containerRegistry.remove(ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, name));
                manager.stop();
                InfinispanLogger.ROOT_LOGGER.infof("Stopped %s cache container", name);
            }
        };

        ServiceName targetName = CAPABILITY.getCapabilityServiceName(context.getCurrentAddress());
        BinderServiceInstaller binderInstaller = new BinderServiceInstaller(InfinispanCacheContainerBindingFactory.EMBEDDED.apply(name), targetName);

        CapabilityServiceInstaller.Builder<EmbeddedCacheManager, EmbeddedCacheManager> builder = CapabilityServiceInstaller.builder(CAPABILITY, wrapper, container);
        for (String alias : aliases) {
            builder.provides(ServiceNameFactory.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER, alias));
            binderInstaller.withAlias(InfinispanCacheContainerBindingFactory.EMBEDDED.apply(alias));
        }
        CapabilityServiceInstaller installer = builder.blocking()
                .requires(List.of(container, loader))
                .onStart(start)
                .onStop(stop)
                .startWhen(StartWhen.AVAILABLE)
                .build();
        return ResourceServiceInstaller.combine(installer, binderInstaller);
    }

    @Listener
    static class CacheLifecycleListener {
        private final ServiceValueRegistry<Cache<?, ?>> registry;
        private final Registrar<String> registrar;
        private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

        CacheLifecycleListener(ServiceValueRegistry<Cache<?, ?>> registry, Registrar<String> registrar) {
            this.registry = registry;
            this.registrar = registrar;
        }

        @CacheStarted
        public CompletionStage<Void> cacheStarted(CacheStartedEvent event) {
            EmbeddedCacheManager container = event.getCacheManager();
            String containerName = container.getCacheManagerConfiguration().cacheManagerName();
            String cacheName = event.getCacheName();
            BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerName, cacheName);
            InfinispanLogger.ROOT_LOGGER.started(configuration);
            this.registrations.put(cacheName, this.registrar.register(cacheName));
            this.registry.add(configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE)).accept(new LazyCache<>(container, cacheName));
            return CompletableFuture.completedStage(null);
        }

        @CacheStopped
        public CompletionStage<Void> cacheStopped(CacheStoppedEvent event) {
            EmbeddedCacheManager container = event.getCacheManager();
            String containerName = container.getCacheManagerConfiguration().cacheManagerName();
            String cacheName = event.getCacheName();
            BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerName, cacheName);
            this.registry.remove(configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE));
            try (Registration registration = this.registrations.remove(cacheName)) {
                InfinispanLogger.ROOT_LOGGER.stopped(configuration);
            }
            return CompletableFuture.completedStage(null);
        }
    }
}
