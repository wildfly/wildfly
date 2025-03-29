/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute.ALIASES;

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
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.manager.DefaultCacheContainer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheContainerServiceConfigurator implements ResourceServiceConfigurator {

    private final RuntimeCapability<Void> capability;
    private final ServiceValueRegistry<Cache<?, ?>> registry;

    public CacheContainerServiceConfigurator(RuntimeCapability<Void> capability, ServiceValueRegistry<Cache<?, ?>> registry) {
        this.capability = capability;
        this.registry = registry;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<ModelNode> aliases = ALIASES.resolveModelAttribute(context, model).asListOrEmpty();

        Object listener = new CacheLifecycleListener(name, this.registry, (CacheContainerResource) context.readResource(PathAddress.EMPTY_ADDRESS));

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
                manager.addListener(listener);
                InfinispanLogger.ROOT_LOGGER.infof("Started %s cache container", name);
            }
        };
        Consumer<EmbeddedCacheManager> stop = new Consumer<>() {
            @Override
            public void accept(EmbeddedCacheManager manager) {
                manager.removeListener(listener);
                manager.stop();
                InfinispanLogger.ROOT_LOGGER.infof("Stopped %s cache container", name);
            }
        };
        CapabilityServiceInstaller.Builder<EmbeddedCacheManager, EmbeddedCacheManager> builder = CapabilityServiceInstaller.builder(this.capability, wrapper, container);
        for (ModelNode alias : aliases) {
            builder.provides(context.getCapabilityServiceSupport().getCapabilityServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER, alias.asString()));
        }
        return builder.blocking()
                .requires(List.of(container, loader))
                .onStart(start)
                .onStop(stop)
                .asPassive()
                .build();
    }

    @Listener
    static class CacheLifecycleListener {
        private final String containerName;
        private final ServiceValueRegistry<Cache<?, ?>> registry;
        private final Registrar<String> registrar;
        private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

        CacheLifecycleListener(String containerName, ServiceValueRegistry<Cache<?, ?>> registry, Registrar<String> registrar) {
            this.containerName = containerName;
            this.registry = registry;
            this.registrar = registrar;
        }

        @CacheStarted
        public CompletionStage<Void> cacheStarted(CacheStartedEvent event) {
            String cacheName = event.getCacheName();
            InfinispanLogger.ROOT_LOGGER.cacheStarted(cacheName, this.containerName);
            this.registrations.put(cacheName, this.registrar.register(cacheName));
            Consumer<Cache<?, ?>> captor = this.registry.add(ServiceDependency.on(InfinispanServiceDescriptor.CACHE, this.containerName, cacheName));
            EmbeddedCacheManager container = event.getCacheManager();
            // Use getCacheAsync(), once available
            BlockingManager blocking = GlobalComponentRegistry.componentOf(container, BlockingManager.class);
            blocking.asExecutor(event.getCacheName()).execute(() -> captor.accept(container.getCache(cacheName)));
            return CompletableFuture.completedStage(null);
        }

        @CacheStopped
        public CompletionStage<Void> cacheStopped(CacheStoppedEvent event) {
            String cacheName = event.getCacheName();
            this.registry.remove(ServiceDependency.on(InfinispanServiceDescriptor.CACHE, this.containerName, cacheName));
            try (Registration registration = this.registrations.remove(cacheName)) {
                InfinispanLogger.ROOT_LOGGER.cacheStopped(cacheName, this.containerName);
            }
            return CompletableFuture.completedStage(null);
        }
    }
}
