/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.ConfigurationManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.ServerSuspendController;
import org.jboss.as.server.suspend.SuspendPriority;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.jboss.as.server.suspend.SuspendableActivityRegistration;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.jboss.logging.Logger;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.cache.infinispan.embedded.AdvancedCacheDecorator;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheContainerConfiguration;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.BlockingLifecycle;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs an MSC service providing an embedded Infinispan cache.
 * @author Paul Ferraro
 */
public class CacheServiceInstaller implements ServiceInstaller {
    private static final Logger LOGGER = Logger.getLogger(CacheServiceInstaller.class);

    private final BinaryServiceConfiguration configuration;

    public CacheServiceInstaller(BinaryServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        ServiceDependency<EmbeddedCacheManager> container = this.configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER);
        ServiceDependency<org.infinispan.configuration.cache.Configuration> cacheConfiguration = this.configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION);
        ServiceDependency<SuspensionStateProvider> suspensionStateProvider = ServiceDependency.on(SuspensionStateProvider.SERVICE_DESCRIPTOR);
        ServiceDependency<SuspendableActivityRegistrar> activityRegistrar = ServiceDependency.on(SuspendableActivityRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<ProcessStateNotifier> processStateProvider = ServiceDependency.on(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        String cacheName = this.configuration.getChildName();
        Supplier<Cache<?, ?>> factory = new Supplier<>() {
            @Override
            public Cache<?, ?> get() {
                EmbeddedCacheManager manager = container.get();
                // If suspended during cache startup, consider switching to suspended configuration
                if (suspensionStateProvider.get().getState() != ServerSuspendController.State.RUNNING) {
                    ControlledProcessState.State state = processStateProvider.get().getCurrentState();
                    // If server is suspended, but will not auto-resume, pre-emptively switch to suspended configuration
                    if ((state == ControlledProcessState.State.RUNNING) || ((state == ControlledProcessState.State.STARTING) && environment.get().isStartSuspended())) {
                        org.infinispan.configuration.cache.Configuration originalConfiguration = cacheConfiguration.get();
                        org.infinispan.configuration.cache.Configuration suspendedConfiguration = createSuspendedConfiguration(originalConfiguration);
                        if (originalConfiguration != suspendedConfiguration) {
                            LOGGER.debugf("%s cache of %s container will start using a suspended configuration", cacheName, manager.getCacheManagerConfiguration().cacheManagerName());
                            updateConfiguration(manager, cacheName, suspendedConfiguration);
                        }
                    }
                }
                return new RestartableCache<>(manager.getCache(cacheName));
            }
        };
        Function<Cache<?, ?>, BlockingLifecycle> lifecycle = new Function<>() {
            @Override
            public BlockingLifecycle apply(Cache<?, ?> cache) {
                org.infinispan.configuration.cache.Configuration originalConfiguration = cacheConfiguration.get();
                org.infinispan.configuration.cache.Configuration suspendedConfiguration = createSuspendedConfiguration(originalConfiguration);
                SuspendableActivity activity = (originalConfiguration != suspendedConfiguration) ? new SuspendableActivity() {
                    private final BlockingManager blocking = GlobalComponentRegistry.componentOf(cache.getCacheManager(), BlockingManager.class);

                    @Override
                    public CompletionStage<Void> suspend(ServerSuspendContext context) {
                        // N.B. Skip configuration swapping if:
                        //  * server is stopping
                        //  * cache already uses suspended configuration
                        //  * cache already uses its original configuration
                        //  * we are the only member of the cache topology
                        if (context.isStopping() || this.inUse(suspendedConfiguration) || (cache.getAdvancedCache().getDistributionManager().getCacheTopology().getActualMembers().size() < 2)) {
                            return SuspendableActivity.COMPLETED;
                        }
                        return this.blocking.runBlocking(new Runnable() {
                            @Override
                            public void run() {
                                LOGGER.debugf("Restarting %s cache of %s container using suspended configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                                updateConfiguration(cache, suspendedConfiguration);
                            }
                        }, "suspend");
                    }

                    @Override
                    public CompletionStage<Void> resume(ServerResumeContext context) {
                        // N.B. Skip configuration swapping if:
                        //  * server is starting, and thus we were never suspended
                        //  * cache already uses its original configuration
                        if (context.isStarting() || this.inUse(originalConfiguration)) {
                            return SuspendableActivity.COMPLETED;
                        }
                        return this.blocking.runBlocking(new Runnable() {
                            @Override
                            public void run() {
                                LOGGER.debugf("Restarting %s cache of %s container using original configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                                updateConfiguration(cache, originalConfiguration);
                            }
                        }, "resume");
                    }

                    boolean inUse(org.infinispan.configuration.cache.Configuration configuration) {
                        return GlobalComponentRegistry.componentOf(cache.getCacheManager(), ConfigurationManager.class).getConfiguration(cache.getName()) == configuration;
                    }
                } : null;
                return new BlockingLifecycle() {
                    private final AtomicReference<SuspendableActivityRegistration> registration = new AtomicReference<>();

                    @Override
                    public boolean isStarted() {
                        return (activity != null) ? (this.registration.get() != null) : cache.getStatus().allowInvocations();
                    }

                    @Override
                    public void start() {
                        try (SuspendableActivityRegistration registration = (activity != null) ? this.registration.getAndSet(activityRegistrar.get().register(activity, SuspendPriority.LAST)) : null) {
                            cache.start();
                        }
                    }

                    @Override
                    public void stop() {
                        try (SuspendableActivityRegistration registration = (activity != null) ? this.registration.getAndSet(null) : null) {
                            cache.stop();
                        }
                    }
                };
            }
        };
        return ServiceInstaller.BlockingBuilder.of(factory, container.map(EmbeddedCacheContainerConfiguration::of).map(EmbeddedCacheContainerConfiguration::getExecutor))
                .map(ManagedCache::new)
                .provides(this.configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE))
                .requires(List.of(cacheConfiguration, activityRegistrar, suspensionStateProvider, processStateProvider, environment))
                .withLifecycle(lifecycle)
                .build()
                .install(target);
    }

    static org.infinispan.configuration.cache.Configuration createSuspendedConfiguration(org.infinispan.configuration.cache.Configuration configuration) {
        // Create a suspended configuration
        // For distributed caches use smallest positive non-zero capacity.
        // TODO Consider handling replicated/invalidation caches via zero capacity, though this will likely require special handling in CacheRegistry
        return configuration.clustering().cacheMode().isDistributed() ? new ConfigurationBuilder().read(configuration).clustering().hash().capacityFactor(Float.MIN_VALUE).build() : configuration;
    }

    static void updateConfiguration(Cache<?, ?> cache, org.infinispan.configuration.cache.Configuration configuration) {
        ComponentRegistry.componentOf(cache, BasicComponentRegistry.class).replaceComponent(Configuration.class.getName(), configuration, false);
        cache.stop();
        updateConfiguration(cache.getCacheManager(), cache.getName(), configuration);
        cache.start();
    }

    static void updateConfiguration(EmbeddedCacheManager manager, String name, org.infinispan.configuration.cache.Configuration configuration) {
        ConfigurationManager configManager = GlobalComponentRegistry.componentOf(manager, ConfigurationManager.class);
        configManager.removeConfiguration(name);
        configManager.putConfiguration(name, configuration);
    }

    private static class RestartableCache<K, V> extends AdvancedCacheDecorator<K, V> {

        private final Map<Object, CacheEventFilter<? super K, ? super V>> listeners;

        RestartableCache(Cache<K, V> cache) {
            this(cache, Collections.synchronizedMap(new IdentityHashMap<>()));
        }

        private RestartableCache(Cache<K, V> cache, Map<Object, CacheEventFilter<? super K, ? super V>> listeners) {
            super(cache.getAdvancedCache(), decorated -> new RestartableCache<>(decorated, listeners));
            this.listeners = listeners;
        }

        @Override
        public void start() {
            // EmbeddedCacheManager.getCache(...) may have already started the cache
            if (!super.getStatus().allowInvocations()) {
                super.start();
            }
            // Restore recorded listeners
            synchronized (this.listeners) {
                for (Map.Entry<Object, CacheEventFilter<? super K, ? super V>> entry : this.listeners.entrySet()) {
                    this.addListener(entry.getKey(), entry.getValue(), null);
                }
            }
        }

        @Override
        public void stop() {
            // Remove recorded listeners (to be restored following restart)
            synchronized (this.listeners) {
                for (Object listener : this.listeners.keySet()) {
                    this.removeListener(listener);
                }
            }
            if (super.getStatus().allowInvocations()) {
                super.stop();
            }
        }

        @Override
        public void addListener(Object listener) {
            this.addListenerAsync(listener).toCompletableFuture().join();
        }

        @Override
        public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
            this.addListenerAsync(listener, filter, converter).toCompletableFuture().join();
        }

        @Override
        public CompletionStage<Void> addListenerAsync(Object listener) {
            return super.addListenerAsync(listener, new CacheEventFilter<>() {
                @Override
                public boolean accept(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata, EventType eventType) {
                    return true;
                }
            }, null);
        }

        @Override
        public <C> CompletionStage<Void> addListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
            // Record listener to be be auto-removed/restored on stop/start
            return super.addListenerAsync(listener, filter, converter).thenAccept(ignore -> this.listeners.put(listener, filter));
        }

        @Override
        public void removeListener(Object listener) {
            this.removeListenerAsync(listener).toCompletableFuture().join();
        }

        @Override
        public CompletionStage<Void> removeListenerAsync(Object listener) {
            // Also remove recorded listener
            return super.removeListenerAsync(listener).thenAccept(ignore -> this.listeners.remove(listener));
        }
    }

    private static class ManagedCache<K, V> extends AdvancedCacheDecorator<K, V> {

        ManagedCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        private ManagedCache(AdvancedCache<K, V> cache) {
            super(cache, ManagedCache::new);
        }

        @Override
        public void start() {
            // No-op.  Lifecycle managed by container.
        }

        @Override
        public void stop() {
            // No-op.  Lifecycle managed by container.
        }
    }
}
