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
package org.wildfly.clustering.web.infinispan.session;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryBuilder;

/**
 * Service building strategy the Infinispan session manager factory.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactoryBuilder implements SessionManagerFactoryBuilder {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    @Override
    public ServiceBuilder<SessionManagerFactory> buildDeploymentDependency(ServiceTarget target, ServiceName name, ServiceName deploymentServiceName, Module module, JBossWebMetaData metaData) {
        ServiceName templateCacheServiceName = getCacheServiceName(metaData.getReplicationConfig());
        String templateCacheName = templateCacheServiceName.getSimpleName();
        ServiceName containerServiceName = templateCacheServiceName.getParent();
        String containerName = containerServiceName.getSimpleName();
        ServiceName templateCacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, templateCacheName);
        String host = deploymentServiceName.getParent().getSimpleName();
        String contextPath = deploymentServiceName.getSimpleName();
        StringBuilder cacheNameBuilder = new StringBuilder(host).append(contextPath);
        if (contextPath.isEmpty() || contextPath.equals("/")) {
            cacheNameBuilder.append("ROOT");
        }
        String cacheName = cacheNameBuilder.toString();
        ServiceName cacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, cacheName);
        ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);

        InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        InjectedValue<Configuration> config = new InjectedValue<>();
        target.addService(cacheConfigurationServiceName, new SessionCacheConfigurationService(cacheName, container, config, metaData))
                .addDependency(containerServiceName, EmbeddedCacheManager.class, container)
                .addDependency(templateCacheConfigurationServiceName, Configuration.class, config)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        final InjectedValue<EmbeddedCacheManager> cacheContainer = new InjectedValue<>();
        CacheService.Dependencies dependencies = new CacheService.Dependencies() {
            @Override
            public EmbeddedCacheManager getCacheContainer() {
                return cacheContainer.getValue();
            }

            @Override
            public XAResourceRecoveryRegistry getRecoveryRegistry() {
                return null;
            }
        };
        AsynchronousService.addService(target, cacheServiceName, new CacheService<>(cacheName, dependencies))
                .addAliases(RouteLocatorService.getCacheServiceName(deploymentServiceName))
                .addDependency(cacheConfigurationServiceName)
                .addDependency(containerServiceName, EmbeddedCacheManager.class, cacheContainer)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;

        @SuppressWarnings("rawtypes")
        InjectedValue<Cache> cache = new InjectedValue<>();
        InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
        InfinispanSessionManagerFactory factory = new InfinispanSessionManagerFactory(module, metaData, cache, affinityFactory);
        return target.addService(name, factory)
                .addDependency(cacheServiceName, Cache.class, cache)
                .addDependency(KeyAffinityServiceFactoryService.getServiceName(containerName), KeyAffinityServiceFactory.class, affinityFactory)
        ;
    }

    private static ServiceName getCacheServiceName(ReplicationConfig config) {
        ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
        String cacheName = (config != null) ? config.getCacheName() : null;
        ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        return (serviceName.length() < 4) ? serviceName.append(CacheContainer.DEFAULT_CACHE_ALIAS) : serviceName;
    }
}
