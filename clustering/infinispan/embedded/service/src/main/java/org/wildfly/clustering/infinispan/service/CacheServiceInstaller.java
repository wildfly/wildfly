/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
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
        Supplier<Cache<?, ?>> cache = new Supplier<>() {
            @Override
            public Cache<?, ?> get() {
                EmbeddedCacheManager manager = container.get();
                org.infinispan.configuration.cache.Configuration originalConfiguration = cacheConfiguration.get();
                SuspendableActivityRegistry registry = activityRegistry.get();
                // Create a suspended configuration
                // For distributed caches use smallest positive non-zero capacity.
                // TODO Consider handling replicated/invalidation caches via zero capacity, though this will likely require special handling in CacheRegistry
                org.infinispan.configuration.cache.Configuration suspendedConfiguration = originalConfiguration.clustering().cacheMode().isDistributed() ? new ConfigurationBuilder().read(originalConfiguration).clustering().hash().capacityFactor(Float.MIN_VALUE).build() : originalConfiguration;
                // If we are starting in suspended mode, switch to suspended configuration
                if ((suspendedConfiguration != originalConfiguration) && (registry.getState() != ServerSuspendController.State.RUNNING)) {
                    ControlledProcessState.State state = processStateProvider.get().getCurrentState();
                    // If server is suspended, but will not auto-resume (e.g. server startup), start cache using suspended configuration
                    if ((state == ControlledProcessState.State.RUNNING) || ((state == ControlledProcessState.State.STARTING) && environment.get().isStartSuspended())) {
                        LOGGER.debugf("%s cache of %s container will start using a suspended configuration", cacheName, manager.getCacheManagerConfiguration().cacheManagerName());
                        manager.undefineConfiguration(cacheName);
                        manager.defineConfiguration(cacheName, suspendedConfiguration);
                    }
                }
                return new DefaultCache<>(manager.getCache(cacheName), originalConfiguration, suspendedConfiguration, registry);
            }
        };
        return ServiceInstaller.builder(ManagedCache::new, cache).blocking()
                .provides(this.configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE))
                .requires(List.of(container, cacheConfiguration, activityRegistry, processStateProvider, environment))
                .onStart(Cache::start)
                .onStop(Cache::stop)
                .build()
                .install(target);
    }

    private static class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
        private final SuspendableActivityRegistry activityRegistry;
        private final SuspendableActivity activity;

        DefaultCache(Cache<K, V> cache, org.infinispan.configuration.cache.Configuration resumedConfiguration, org.infinispan.configuration.cache.Configuration suspendedConfiguration, SuspendableActivityRegistry activityRegistry) {
            this(cache.getAdvancedCache(), activityRegistry, new SuspendableActivity() {
                private final BlockingManager blocking = GlobalComponentRegistry.componentOf(cache.getCacheManager(), BlockingManager.class);
                @Override
                public CompletionStage<Void> suspend(ServerSuspendContext context) {
                    // N.B. Only switch to a suspended configuration if there are other cluster members in the cache topology.
                    if (context.isStarting() || context.isStopping() || (resumedConfiguration == suspendedConfiguration) || (cache.getCacheConfiguration().clustering().hash().capacityFactor() == suspendedConfiguration.clustering().hash().capacityFactor()) || (cache.getAdvancedCache().getDistributionManager().getCacheTopology().getActualMembers().size() < 2)) {
                        return SuspendableActivity.COMPLETED;
                    }
                    return this.blocking.runBlocking(new Runnable() {
                        @Override
                        public void run() {
                            LOGGER.debugf("Restarting %s cache of %s container using suspended configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                            ComponentRegistry.componentOf(cache, BasicComponentRegistry.class).replaceComponent(Configuration.class.getName(), suspendedConfiguration, false);
                            cache.stop();
                            cache.start();
                        }
                    }, "suspend");
                }

                @Override
                public CompletionStage<Void> resume(ServerResumeContext context) {
                    if (context.isStarting() || (resumedConfiguration == suspendedConfiguration) || (cache.getCacheConfiguration().clustering().hash().capacityFactor() == resumedConfiguration.clustering().hash().capacityFactor())) {
                        return SuspendableActivity.COMPLETED;
                    }
                    return this.blocking.runBlocking(new Runnable() {
                        @Override
                        public void run() {
                            LOGGER.debugf("Restarting %s cache of %s container using original configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                            ComponentRegistry.componentOf(cache, BasicComponentRegistry.class).replaceComponent(Configuration.class.getName(), resumedConfiguration, false);
                            cache.stop();
                            cache.start();
                        }
                    }, "resume");
                }
            });
        }

        private DefaultCache(AdvancedCache<K, V> cache, SuspendableActivityRegistry activityRegistry, SuspendableActivity activity) {
            super(cache.getAdvancedCache());
            this.activityRegistry = activityRegistry;
            this.activity = activity;
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public AdvancedCache rewrap(AdvancedCache delegate) {
            return new DefaultCache<>(delegate, this.activityRegistry, this.activity);
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
