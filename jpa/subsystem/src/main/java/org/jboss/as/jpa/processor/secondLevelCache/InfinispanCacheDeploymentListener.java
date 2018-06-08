/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.processor.secondLevelCache;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.security.AccessController;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;

/**
 * InfinispanCacheDeploymentListener adds Infinispan second level cache dependencies during application deployment.
 *
 * @author Scott Marlow
 * @author Paul Ferraro
 */
public class InfinispanCacheDeploymentListener implements EventListener {

    public static final String CACHE_TYPE = "cachetype";    // shared (jpa) or private (for native applications)
    public static final String CACHE_PRIVATE = "private";
    public static final String CONTAINER = "container";
    public static final String NAME = "name";
    public static final String CACHES = "caches";

    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";

    @Override
    public void beforeEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public void afterEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public Wrapper startCache(Classification classification, Properties properties) throws Exception {
        ServiceContainer target = currentServiceContainer();
        String container = properties.getProperty(CONTAINER);
        String cacheType = properties.getProperty(CACHE_TYPE);
        // TODO Figure out how to access CapabilityServiceSupport from here
        ServiceName containerServiceName = ServiceName.parse(InfinispanRequirement.CONTAINER.resolve(container));

        // need a private cache for non-jpa application use
        String name = properties.getProperty(NAME, UUID.randomUUID().toString());

        ServiceBuilder<?> builder = target.addService(ServiceName.JBOSS.append(DEFAULT_CACHE_CONTAINER, name));
        Supplier<EmbeddedCacheManager> manager = builder.requires(containerServiceName);

        if (CACHE_PRIVATE.equals(cacheType)) {
            // If using a private cache, addCacheDependencies(...) is never triggered
            String[] caches = properties.getProperty(CACHES).split("\\s+");
            for (String cache : caches) {
                ServiceName dependencyName = ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(container, cache));
                builder.requires(dependencyName);
            }
        }

        ServiceController<?> controller = builder.install();

        // Ensure cache configuration services are started
        StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            monitor.awaitStability();
            // TODO Figure out why the awaitStability() returns prematurely while there are still dependencies starting
            while (controller.getState() == ServiceController.State.DOWN) {
                Thread.yield();
                monitor.awaitStability();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            monitor.removeController(controller);
        }

        return new CacheWrapper(manager.get(), controller);
    }

    @Override
    public void addCacheDependencies(Classification classification, Properties properties) {
        ServiceBuilder<?> builder = CacheDeploymentListener.getInternalDeploymentServiceBuilder();
        CapabilityServiceSupport support = CacheDeploymentListener.getInternalDeploymentCapablityServiceSupport();
        String container = properties.getProperty(CONTAINER);
        for (String cache : properties.getProperty(CACHES).split("\\s+")) {
            builder.addDependency(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, container, cache));
        }
    }

    @Override
    public void stopCache(Classification classification, Wrapper wrapper) {
        // Remove services created in startCache(...)
        ((CacheWrapper) wrapper).close();
    }

    private static class CacheWrapper implements Wrapper, AutoCloseable {

        private final EmbeddedCacheManager embeddedCacheManager;
        private final ServiceController<?> controller;

        CacheWrapper(EmbeddedCacheManager embeddedCacheManager, ServiceController<?> controller) {
            this.embeddedCacheManager = embeddedCacheManager;
            this.controller = controller;
        }

        @Override
        public Object getValue() {
            return this.embeddedCacheManager;
        }

        @Override
        public void close() {
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.tracef("stop second level cache by removing dependency on service '%s'", this.controller.getName().getCanonicalName());
            }
            StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(this.controller);
            this.controller.setMode(ServiceController.Mode.REMOVE);
            try {
                monitor.awaitStability();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                monitor.removeController(this.controller);
            }
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
