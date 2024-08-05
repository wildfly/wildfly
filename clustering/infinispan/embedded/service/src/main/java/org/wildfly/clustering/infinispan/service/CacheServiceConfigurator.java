/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.ServerSuspendController;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheServiceConfigurator<K, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Cache<K, V>> {
    private static final Logger LOGGER = Logger.getLogger(CacheServiceConfigurator.class);

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<EmbeddedCacheManager> container;
    private volatile SupplierDependency<Configuration> configuration;
    private volatile SupplierDependency<SuspendableActivityRegistry> activityRegistry;
    private volatile SupplierDependency<ServerEnvironment> environment;

    public CacheServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Cache<K, V> get() {
        String cacheName = this.cacheName;
        EmbeddedCacheManager container = this.container.get();
        Configuration originalConfiguration = this.configuration.get();
        SuspendableActivityRegistry activityRegistry = this.activityRegistry.get();
        // Create a suspended configuration using smallest positive non-zero capacity.
        Configuration suspendedConfiguration = originalConfiguration.clustering().cacheMode().isClustered() ? new ConfigurationBuilder().read(originalConfiguration).clustering().hash().capacityFactor(Float.MIN_VALUE).build() : originalConfiguration;
        // If we are starting in suspended mode, switch to suspended configuration
        if ((activityRegistry.getState() != ServerSuspendController.State.RUNNING) && this.environment.get().isStartSuspended() && (suspendedConfiguration != originalConfiguration)) {
            LOGGER.debugf("%s cache of %s container will start using a suspended configuration", cacheName, container.getCacheManagerConfiguration().cacheManagerName());
            container.undefineConfiguration(cacheName);
            container.defineConfiguration(cacheName, suspendedConfiguration);
        }
        Cache<K, V> cache = new DefaultCache<>(container.getCache(cacheName), originalConfiguration, suspendedConfiguration, activityRegistry);
        cache.start();
        return cache;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.container = new ServiceSupplierDependency<>(InfinispanRequirement.CONTAINER.getServiceName(support, this.containerName));
        this.configuration = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, this.containerName, this.cacheName));
        this.activityRegistry = new ServiceSupplierDependency<>(support.getCapabilityServiceName(SuspendableActivityRegistry.SERVICE_DESCRIPTOR));
        this.environment = new ServiceSupplierDependency<>(support.getCapabilityServiceName(ServerEnvironment.SERVICE_DESCRIPTOR));
        return this;
    }

    @Override
    public final ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<Cache<K, V>> cache = new CompositeDependency(this.configuration, this.container, this.activityRegistry, this.environment).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(cache, ManagedCache::new, this, Cache::stop);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    private static class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
        private final SuspendableActivityRegistry activityRegistry;
        private final SuspendableActivity activity;

        DefaultCache(Cache<K, V> cache, Configuration resumedConfiguration, Configuration suspendedConfiguration, SuspendableActivityRegistry activityRegistry) {
            this(cache.getAdvancedCache(), activityRegistry, new SuspendableActivity() {
                @Override
                public CompletionStage<Void> suspend(ServerSuspendContext context) {
                    if (!context.isStarting() && !context.isStopping() && (resumedConfiguration != suspendedConfiguration) && !cache.getCacheConfiguration().clustering().hash().matches(suspendedConfiguration.clustering().hash())) {
                        LOGGER.debugf("Restarting %s cache of %s container using suspended configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                        // TODO Use non-blocking stop w/Infinispan 15
                        cache.stop();
                        cache.getCacheManager().undefineConfiguration(cache.getName());
                        cache.getCacheManager().defineConfiguration(cache.getName(), suspendedConfiguration);
                        // TODO Use non-blocking start w/Infinispan 15
                        cache.start();
                    }
                    return SuspendableActivity.COMPLETED;
                }

                @Override
                public CompletionStage<Void> resume(ServerResumeContext context) {
                    if (!context.isStarting() && (resumedConfiguration != suspendedConfiguration) && !cache.getCacheConfiguration().clustering().hash().matches(resumedConfiguration.clustering().hash())) {
                        LOGGER.debugf("Restarting %s cache of %s container using original configuration", cache.getName(), cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName());
                        // TODO Use non-blocking stop w/Infinispan 15
                        cache.stop();
                        cache.getCacheManager().undefineConfiguration(cache.getName());
                        cache.getCacheManager().defineConfiguration(cache.getName(), resumedConfiguration);
                        // TODO Use non-blocking start w/Infinispan 15
                        cache.start();
                    }
                    return SuspendableActivity.COMPLETED;
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
            super(cache.getAdvancedCache());
        }

        @SuppressWarnings("unchecked")
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