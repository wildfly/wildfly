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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.service.ServiceDependency;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryServiceConfiguratorFactory<I> implements BeanManagerFactoryServiceConfiguratorFactory {

    static String getCacheName(ServiceName deploymentUnitServiceName, String beanManagerFactoryName) {
        List<String> parts = new ArrayList<>(3);
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            parts.add(deploymentUnitServiceName.getParent().getSimpleName());
        }
        parts.add(deploymentUnitServiceName.getSimpleName());
        parts.add(beanManagerFactoryName);
        return String.join("/", parts);
    }

    private final String name;
    private final BeanManagerFactoryServiceConfiguratorConfiguration config;

    public InfinispanBeanManagerFactoryServiceConfiguratorFactory(String name, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(final ServiceName name) {
        String cacheName = getCacheName(name, this.name);
        String containerName = this.config.getContainerName();
        String templateCacheName = this.config.getCacheName();

        // Ensure eviction and expiration are disabled
        Consumer<ConfigurationBuilder> configurator = builder -> {
            // Ensure expiration is not enabled on cache
            ExpirationConfiguration expiration = builder.expiration().create();
            if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                builder.expiration().lifespan(-1).maxIdle(-1);
                InfinispanEjbLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, templateCacheName));
            }

            int size = this.config.getMaxSize();
            EvictionStrategy strategy = (size > 0) ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
            builder.memory().evictionStrategy(strategy).evictionType(EvictionType.COUNT).storageType(StorageType.OBJECT).size(size);
            if (strategy.isEnabled()) {
                // Only evict bean group entries
                // We will cascade eviction to the associated beans
                builder.addModule(DataContainerConfigurationBuilder.class).evictable(BeanGroupKey.class::isInstance);
            }
        };

        CapabilityServiceConfigurator configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CONFIGURATION.getName()).append(containerName, cacheName), containerName, cacheName, templateCacheName, configurator);
        CapabilityServiceConfigurator cacheConfigurator = new CacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CACHE.getName()).append(containerName, cacheName), containerName, cacheName).require(new ServiceDependency(name.append("marshalling")));
        return Arrays.asList(configurationConfigurator, cacheConfigurator);
    }

    @Override
    public CapabilityServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanContext context) {
        return new InfinispanBeanManagerFactoryServiceConfigurator<>(this.name, context, this.config);
    }
}
