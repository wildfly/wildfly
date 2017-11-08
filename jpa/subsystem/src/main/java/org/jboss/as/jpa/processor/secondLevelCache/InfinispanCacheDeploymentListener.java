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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;

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
        String cache_type = properties.getProperty(CACHE_TYPE);
        String container = properties.getProperty(CONTAINER);
        // TODO Figure out how to access CapabilityServiceSupport from here
        ServiceName containerServiceName = ServiceName.parse(InfinispanRequirement.CONTAINER.resolve(container));
        List<ServiceName> serviceNames = new LinkedList<>();
        EmbeddedCacheManager embeddedCacheManager;
        if (CACHE_PRIVATE.equals(cache_type)) {
            // need a private cache for non-jpa application use
            String name = properties.getProperty(NAME, UUID.randomUUID().toString());
            ServiceName serviceName = ServiceName.JBOSS.append(DEFAULT_CACHE_CONTAINER, name);
            serviceNames.add(serviceName);
            ServiceTarget target = currentServiceContainer();

            String[] caches = properties.getProperty(CACHES).split("\\s+");
            List<ServiceController<Configuration>> controllers = new ArrayList<>(caches.length);
            for (String cache : caches) {
                ServiceName aliasServiceName = serviceName.append(cache);
                ServiceName configServiceName = ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(container, cache));
                controllers.add(new AliasServiceBuilder<>(aliasServiceName, configServiceName, Configuration.class).build(target)
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install());
            }

            // Create a mock service that represents this session factory instance
            embeddedCacheManager = new AliasServiceBuilder<>(serviceName, containerServiceName, EmbeddedCacheManager.class).build(target)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install()
                    .awaitValue();

            // Ensure cache configuration services are started
            for (ServiceController<Configuration> controller : controllers) {
                serviceNames.add(controller.getName());
                controller.awaitValue();
            }
        } else {
            // need a shared cache for jpa applications
            ServiceRegistry registry = currentServiceContainer();
            embeddedCacheManager = (EmbeddedCacheManager) registry.getRequiredService(containerServiceName).getValue();
        }
        return new CacheWrapper(embeddedCacheManager, serviceNames);
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
    public void stopCache(Classification classification, Wrapper wrapper, boolean ignoreStop) {
        // Remove services created in startCache(...)
        CacheWrapper cacheWrapper = (CacheWrapper) wrapper;
        ServiceRegistry registry = currentServiceContainer();
        for (ServiceName serviceName : cacheWrapper.getServiceNames()) {
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.tracef("stop second level cache by removing dependency on service '%s'", serviceName.getCanonicalName());
            }
            ServiceController<?> service = registry.getService(serviceName);
            if (service != null) {
                ServiceContainerHelper.remove(service);
            }
        }
    }

    private static class CacheWrapper implements Wrapper {

        CacheWrapper(EmbeddedCacheManager embeddedCacheManager, Collection<ServiceName> serviceNames) {
            this.embeddedCacheManager = embeddedCacheManager;
            this.serviceNames = serviceNames;
        }

        private final EmbeddedCacheManager embeddedCacheManager;
        private final Collection<ServiceName> serviceNames;

        @Override
        public Object getValue() {
            return embeddedCacheManager;
        }

        Collection<ServiceName> getServiceNames() {
            return this.serviceNames;
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
