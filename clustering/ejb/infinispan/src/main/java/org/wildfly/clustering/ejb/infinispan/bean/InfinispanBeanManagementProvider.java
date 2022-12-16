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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.List;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.FunctionalCapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanDeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.common.function.Functions;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 */
public class InfinispanBeanManagementProvider implements BeanManagementProvider {

    private final String name;
    private final InfinispanBeanManagementConfiguration config;

    public InfinispanBeanManagementProvider(String name, InfinispanBeanManagementConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(BeanDeploymentConfiguration configuration) {
        String containerName = this.config.getContainerName();
        String templateCacheName = this.config.getCacheName();
        String cacheName = configuration.getDeploymentName();

        // Ensure eviction and expiration are disabled
        Consumer<ConfigurationBuilder> configurator = builder -> {
            // Ensure expiration is not enabled on cache
            ExpirationConfiguration expiration = builder.expiration().create();
            if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                builder.expiration().lifespan(-1).maxIdle(-1);
                InfinispanEjbLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, templateCacheName));
            }

            Integer size = this.config.getMaxActiveBeans();
            EvictionStrategy strategy = (size != null) ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
            builder.memory().storage(StorageType.HEAP).whenFull(strategy).maxCount((size != null) ? size.longValue() : 0);
            if (strategy.isEnabled()) {
                // Only evict bean group entries
                // We will cascade eviction to the associated beans
                builder.addModule(DataContainerConfigurationBuilder.class).evictable(InfinispanBeanGroupKey.class::isInstance);
            }
        };

        CapabilityServiceConfigurator configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CONFIGURATION.getName()).append(containerName, cacheName), containerName, cacheName, templateCacheName, configurator);
        CapabilityServiceConfigurator cacheConfigurator = new CacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CACHE.getName()).append(containerName, cacheName), containerName, cacheName);
        CapabilityServiceConfigurator groupConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedCacheGroupServiceConfiguratorProvider.class, containerName, cacheName);
        CapabilityServiceConfigurator marshallerConfigurator = new FunctionalCapabilityServiceConfigurator<>(configuration.getDeploymentServiceName().append(this.name, "marshaller"), this.config.getMarshallerFactory(), Functions.constantSupplier(configuration));
        CapabilityServiceConfigurator groupManagerConfigurator = new InfinispanBeanGroupManagerServiceConfigurator<>(configuration, new ServiceSupplierDependency<>(cacheConfigurator), new ServiceSupplierDependency<>(marshallerConfigurator));
        CapabilityServiceConfigurator groupListenerConfigurator = new InfinispanBeanGroupListenerServiceConfigurator<>(cacheConfigurator.getServiceName().append("listener"), new ServiceSupplierDependency<>(cacheConfigurator), new ServiceSupplierDependency<>(marshallerConfigurator));
        return List.of(configurationConfigurator, cacheConfigurator, groupConfigurator, marshallerConfigurator, groupManagerConfigurator, groupListenerConfigurator);
    }

    @Override
    public CapabilityServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanConfiguration context) {
        return new InfinispanBeanManagerFactoryServiceConfigurator<>(context, this.config);
    }
}
