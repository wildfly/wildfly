package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.factories.GlobalComponentRegistry;
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

    public GlobalComponentRegistryService(Value<CacheContainer> manager) {
        this.manager = manager;
    }

    @Override
    public GlobalComponentRegistry getValue() {
        return this.manager.getValue().getGlobalComponentRegistry();
    }

    @Override
    public void start(StartContext context) {
        this.getValue().start();
    }

    @Override
    public void stop(StopContext context) {
        this.getValue().stop();
    }
}
