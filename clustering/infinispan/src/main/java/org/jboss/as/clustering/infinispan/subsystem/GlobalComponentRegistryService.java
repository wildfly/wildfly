package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

public class GlobalComponentRegistryService implements Service<GlobalComponentRegistry> {

    public static ServiceName getServiceName(String container) {
        return EmbeddedCacheManagerService.getServiceName(container).append("global-component-registry");
    }

    private final Value<CacheContainer> manager;
    private volatile GlobalComponentRegistry registry;

    public GlobalComponentRegistryService(Value<CacheContainer> manager) {
        this.manager = manager;
    }

    @Override
    public GlobalComponentRegistry getValue() {
        return this.registry;
    }

    @Override
    public void start(StartContext context) {
        this.registry = this.manager.getValue().getGlobalComponentRegistry();
        this.registry.start();
    }

    @Override
    public void stop(StopContext context) {
        EmbeddedCacheManager manager = this.manager.getValue();
        // If a cache was not started via the CacheService, it may still be running.
        // If so, let the EmbeddedCacheManagerService stop the GlobalComponentRegistry.
        for (String cacheName: manager.getCacheNames()) {
            if (manager.isRunning(cacheName)) return;
        }
        this.registry.stop();
        this.registry = null;
    }
}
