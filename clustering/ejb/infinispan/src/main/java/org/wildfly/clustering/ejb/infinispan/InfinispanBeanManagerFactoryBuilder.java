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
package org.wildfly.clustering.ejb.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilder;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryBuilder<G, I> implements BeanManagerFactoryBuilder<G, I> {

    private final BeanManagerFactoryBuilderConfiguration config;

    public InfinispanBeanManagerFactoryBuilder(BeanManagerFactoryBuilderConfiguration config) {
        this.config = config;
    }

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        String cacheName = getCacheName(deploymentUnitServiceName);
        ServiceName configurationServiceName = CacheConfigurationService.getServiceName(this.config.getContainerName(), cacheName);
        final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        InjectedValue<Configuration> configuration = new InjectedValue<>();
        target.addService(configurationServiceName, new BeanCacheConfigurationService(cacheName, container, configuration))
                .addDependency(EmbeddedCacheManagerService.getServiceName(this.config.getContainerName()), EmbeddedCacheManager.class, container)
                .addDependency(CacheConfigurationService.getServiceName(this.config.getContainerName(), this.config.getCacheName()), Configuration.class, configuration)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;

        ServiceName cacheServiceName = CacheService.getServiceName(this.config.getContainerName(), cacheName);
        CacheService.Dependencies dependencies = new CacheService.Dependencies() {
            @Override
            public EmbeddedCacheManager getCacheContainer() {
                return container.getValue();
            }

            @Override
            public XAResourceRecoveryRegistry getRecoveryRegistry() {
                return null;
            }
        };
        AsynchronousService.addService(target, cacheServiceName, new CacheService<>(cacheName, dependencies))
                .addDependency(configurationServiceName)
                .addDependency(deploymentUnitServiceName.append("marshalling-configuration"))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public <T> ServiceBuilder<? extends BeanManagerFactory<G, I, T>> build(ServiceTarget target, ServiceName name, BeanContext context, ServiceName marshallingConfigurationServiceName) {
        @SuppressWarnings("rawtypes")
        InjectedValue<Cache> cache = new InjectedValue<>();
        InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
        InjectedValue<VersionedMarshallingConfiguration> config = new InjectedValue<>();
        InfinispanBeanManagerFactory<G, I, T> factory = new InfinispanBeanManagerFactory<>(context, config, cache, affinityFactory, this.config);
        return target.addService(name, factory)
                .addDependency(CacheService.getServiceName(this.config.getContainerName(), getCacheName(context.getDeploymentUnitServiceName())), Cache.class, cache)
                .addDependency(KeyAffinityServiceFactoryService.getServiceName(this.config.getContainerName()), KeyAffinityServiceFactory.class, affinityFactory)
                .addDependency(marshallingConfigurationServiceName, VersionedMarshallingConfiguration.class, config)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("clustering", "registry", this.config.getContainerName(), "default"), Registry.class, factory.getRegistryInjector())
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("clustering", "nodes", this.config.getContainerName(), "default"), NodeFactory.class, factory.getNodeFactoryInjector())
        ;
    }

    private static String getCacheName(ServiceName deploymentUnitServiceName) {
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            return deploymentUnitServiceName.getParent().getSimpleName() + "/" + deploymentUnitServiceName.getSimpleName();
        }
        return deploymentUnitServiceName.getSimpleName();
    }
}
