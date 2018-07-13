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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ServiceConfiguratorAdapter;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.EvictableDataContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceDependency;
import org.wildfly.clustering.service.concurrent.RemoveOnCancelScheduledExecutorServiceConfigurator;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryServiceConfiguratorFactory<I> implements BeanManagerFactoryServiceConfiguratorFactory {

    private static final ThreadFactory EXPIRATION_THREAD_FACTORY = createThreadFactory();

    private static ThreadFactory createThreadFactory() {
        return AccessController.doPrivileged(new PrivilegedAction<ThreadFactory>() {
            @Override
            public ThreadFactory run() {
                return new JBossThreadFactory(new ThreadGroup(InfinispanBeanManager.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
            }
        });
    }

    static String getCacheName(ServiceName deploymentUnitServiceName) {
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            return deploymentUnitServiceName.getParent().getSimpleName() + "/" + deploymentUnitServiceName.getSimpleName();
        }
        return deploymentUnitServiceName.getSimpleName();
    }

    private final CapabilityServiceSupport support;
    private final String name;
    private final BeanManagerFactoryServiceConfiguratorConfiguration config;

    public InfinispanBeanManagerFactoryServiceConfiguratorFactory(CapabilityServiceSupport support, String name, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        this.support = support;
        this.name = name;
        this.config = config;
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(final ServiceName name) {
        String cacheName = getCacheName(name);
        String containerName = this.config.getContainerName();
        String templateCacheName = this.config.getCacheName();

        // Ensure eviction and expiration are disabled
        @SuppressWarnings("deprecation")
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
                builder.dataContainer().dataContainer(EvictableDataContainer.createDataContainer(builder, size, BeanGroupKey.class::isInstance));
            }
        };

        List<CapabilityServiceConfigurator> builders = new ArrayList<>(4);
        builders.add(new TemplateConfigurationServiceConfigurator(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, cacheName)), containerName, cacheName, templateCacheName, configurator));
        builders.add(new CacheServiceConfigurator<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(containerName, cacheName)), containerName, cacheName).require(new ServiceDependency(name.append("marshalling"))));
        builders.add(new ServiceConfiguratorAdapter(new RemoveOnCancelScheduledExecutorServiceConfigurator(name.append(this.name, "expiration"), EXPIRATION_THREAD_FACTORY)));
        return builders;
    }

    @Override
    public ServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanContext context) {
        return new InfinispanBeanManagerFactoryServiceConfigurator<>(this.support, this.name, context, this.config);
    }
}
