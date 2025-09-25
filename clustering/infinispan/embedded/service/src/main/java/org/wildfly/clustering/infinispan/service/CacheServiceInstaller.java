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
import java.util.function.Supplier;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
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
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.logging.Logger;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
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
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        ServiceDependency<ProcessStateNotifier> processStateProvider = ServiceDependency.on(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        String cacheName = this.configuration.getChildName();
        Supplier<Cache<?, ?>> factory = new Supplier<>() {
            @Override
            public Cache<?, ?> get() {
                EmbeddedCacheManager manager = container.get();
                org.infinispan.configuration.cache.Configuration originalConfiguration = cacheConfiguration.get();
                SuspendableActivityRegistry registry = activityRegistry.get();
                // Create a suspended configuration
                // For distributed caches use smallest positive non-zero capacity.
                // TODO Consider handling replicated/invalidation caches via zero capacity, though this will likely require special handling in CacheRegistry
                org.infinispan.configuration.cache.Configuration suspendedConfiguration = originalConfiguration.clustering().cacheMode().isDistributed() ? new ConfigurationBuilder().read(originalConfiguration).clustering().hash().capacityFactor(Float.MIN_VALUE).build() : originalConfiguration;
                // If we are starting in suspended mode, switch to suspended configuration before starting cache
                if ((suspendedConfiguration != originalConfiguration) && (registry.getState() != ServerSuspendController.State.RUNNING)) {
                    ControlledProcessState.State state = processStateProvider.get().getCurrentState();
                    // If server is suspended, but will not auto-resume (e.g. server startup), pre-emptively switch to suspended configuration
                    if ((state == ControlledProcessState.State.RUNNING) || ((state == ControlledProcessState.State.STARTING) && environment.get().isStartSuspended())) {
                        LOGGER.debugf("%s cache of %s container will start using a suspended configuration", cacheName, manager.getCacheManagerConfiguration().cacheManagerName());
                        updateConfiguration(manager, cacheName, suspendedConfiguration);
                    }
                }
                Cache<?, ?> cache = manager.getCache(cacheName);
                return (suspendedConfiguration != originalConfiguration) ? new SuspendableCache<>(cache, originalConfiguration, suspendedConfiguration, registry) : cache;
            }
        };
        return ServiceInstaller.builder(ManagedCache::new, factory).blocking()
                .provides(this.configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE))
                .requires(List.of(container, cacheConfiguration, activityRegistry, processStateProvider, environment))
                .onStart(Cache::start)
                .onStop(Cache::stop)
                .build()
                .install(target);
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

    private static class SuspendableCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

        private final SuspendableActivityRegistry activityRegistry;
        private final SuspendableActivity activity;
        private final Map<Object, CacheEventFilter<? super K, ? super V>> listeners;

        SuspendableCache(Cache<K, V> cache, org.infinispan.configuration.cache.Configuration originalConfiguration, org.infinispan.configuration.cache.Configuration suspendedConfiguration, SuspendableActivityRegistry activityRegistry) {
            this(cache, originalConfiguration, suspendedConfiguration, activityRegistry, Collections.synchronizedMap(new IdentityHashMap<>()));
        }

        private SuspendableCache(Cache<K, V> cache, org.infinispan.configuration.cache.Configuration originalConfiguration, org.infinispan.configuration.cache.Configuration suspendedConfiguration, SuspendableActivityRegistry activityRegistry, Map<Object, CacheEventFilter<? super K, ? super V>> listeners) {
            this(cache.getAdvancedCache(), activityRegistry, listeners, new SuspendableActivity() {
                private final BlockingManager blocking = GlobalComponentRegistry.componentOf(cache.getCacheManager(), BlockingManager.class);
                @Override
                public CompletionStage<Void> suspend(ServerSuspendContext context) {
                    // N.B. Skip configuration swapping if:
                    //  * server is stopping
                    //  * cache already uses a suspended configuration
                    //  * we are the only cache topology member (and cannot tolerate a cache restart)
                    if (context.isStopping() || this.inUse(suspendedConfiguration) || (cache.getAdvancedCache().getDistributionManager().getCacheTopology().getActualMembers().size() < 2)) {
                        return SuspendableActivity.COMPLETED;
                    }
                    return this.blocking.runBlocking(new Runnable() {
                        @Override
                        public void run() {
                            // Remove recorded listeners (to be restored during resume)
                            synchronized (listeners) {
                                for (Object listener : listeners.keySet()) {
                                    cache.removeListener(listener);
                                }
                            }
                            LOGGER.debugf("Restarting %s cache of %s container using suspended configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                            updateConfiguration(cache, suspendedConfiguration);
                        }
                    }, "suspend");
                }

                @Override
                public CompletionStage<Void> resume(ServerResumeContext context) {
                    // N.B. Skip configuration swapping if cache already uses its original configuration
                    if (this.inUse(originalConfiguration)) {
                        return SuspendableActivity.COMPLETED;
                    }
                    return this.blocking.runBlocking(new Runnable() {
                        @Override
                        public void run() {
                            LOGGER.debugf("Restarting %s cache of %s container using original configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                            updateConfiguration(cache, originalConfiguration);
                            // Restore recorded listeners
                            synchronized (listeners) {
                                for (Map.Entry<Object, CacheEventFilter<? super K, ? super V>> entry : listeners.entrySet()) {
                                    cache.addListener(entry.getKey(), entry.getValue(), null);
                                }
                            }
                        }
                    }, "resume");
                }

                boolean inUse(org.infinispan.configuration.cache.Configuration configuration) {
                    return ComponentRegistry.componentOf(cache, BasicComponentRegistry.class).getComponent(Configuration.class) == configuration;
                }
            });
        }

        private SuspendableCache(AdvancedCache<K, V> cache, SuspendableActivityRegistry activityRegistry, Map<Object, CacheEventFilter<? super K, ? super V>> listeners, SuspendableActivity activity) {
            super(cache.getAdvancedCache());
            this.activityRegistry = activityRegistry;
            this.listeners = listeners;
            this.activity = activity;
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
            // Record listener to be be restored on resume
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

        @SuppressWarnings({ "unchecked" })
        @Override
        public AdvancedCache rewrap(AdvancedCache delegate) {
            return new SuspendableCache<>(delegate, this.activityRegistry, this.listeners, this.activity);
        }

        @Override
        public void start() {
            this.activityRegistry.registerActivity(this.activity, SuspendableActivityRegistry.SuspendPriority.LAST);
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
            this.activityRegistry.unregisterActivity(this.activity);
        }
    }

    private static class ManagedCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

        ManagedCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        private ManagedCache(AdvancedCache<K, V> cache) {
            super(cache);
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public AdvancedCache rewrap(AdvancedCache delegate) {
            return new ManagedCache<>(delegate);
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
