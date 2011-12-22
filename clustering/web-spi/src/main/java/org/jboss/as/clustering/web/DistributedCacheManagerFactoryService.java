package org.jboss.as.clustering.web;

import java.util.ServiceLoader;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class DistributedCacheManagerFactoryService implements Service<DistributedCacheManagerFactory> {
    private final DistributedCacheManagerFactory factory;

    public DistributedCacheManagerFactoryService() {
        this(load());
    }

    public DistributedCacheManagerFactoryService(DistributedCacheManagerFactory factory) {
        this.factory = factory;
    }

    private static DistributedCacheManagerFactory load() {
        for (DistributedCacheManagerFactory manager: ServiceLoader.load(DistributedCacheManagerFactory.class, DistributedCacheManagerFactory.class.getClassLoader())) {
            return manager;
        }
        return null;
    }

    @Override
    public DistributedCacheManagerFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}
