/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.infinispan.configuration.cache.StateTransferConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.spi.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * Creates client mapping services.
 * @author Paul Ferraro
 */
public class ClientMappingsCacheServiceConfiguratorProvider implements CacheServiceConfiguratorProvider, IdentityCacheServiceConfiguratorProvider, Consumer<ConfigurationBuilder> {

    private final Class<? extends CacheServiceConfiguratorProvider> providerClass;

    ClientMappingsCacheServiceConfiguratorProvider(Class<? extends CacheServiceConfiguratorProvider> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServiceConfigurators(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String aliasCacheName) {
        List<CapabilityServiceConfigurator> configurators = new LinkedList<>();
        if (aliasCacheName == null) {
            String cacheName = BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME;
            configurators.add(new TemplateConfigurationServiceConfigurator(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, cacheName)), containerName, cacheName, aliasCacheName, this));
            configurators.add(new CacheServiceConfigurator<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(containerName, cacheName)), containerName, cacheName));
            ServiceNameRegistry<ClusteringCacheRequirement> routingRegistry = requirement -> ServiceName.parse(requirement.resolve(containerName, cacheName));
            for (CacheServiceConfiguratorProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
                configurators.addAll(provider.getServiceConfigurators(routingRegistry, containerName, cacheName));
            }
        }
        return configurators;
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServiceConfigurators(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String cacheName, String targetCacheName) {
        return this.getServiceConfigurators(registry, containerName, cacheName);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ConfigurationBuilder builder) {
        ClusteringConfigurationBuilder clustering = builder.clustering();
        CacheMode mode = clustering.cacheMode();
        clustering.cacheMode(mode.needsStateTransfer() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
        // don't use DefaultConsistentHashFactory for REPL caches (WFLY-9276)
        clustering.hash().consistentHashFactory(null);
        clustering.l1().disable();
        // Workaround for ISPN-8722
        AttributeSet attributes = TemplateConfigurationServiceConfigurator.getAttributes(clustering);
        attributes.attribute(ClusteringConfiguration.BIAS_ACQUISITION).reset();
        attributes.attribute(ClusteringConfiguration.BIAS_LIFESPAN).reset();
        attributes.attribute(ClusteringConfiguration.INVALIDATION_BATCH_SIZE).reset();
        // Ensure we use the default data container
        builder.dataContainer().dataContainer(null);
        // Disable expiration
        builder.expiration().lifespan(-1).maxIdle(-1);
        // Disable eviction
        builder.memory().size(-1).evictionStrategy(EvictionStrategy.MANUAL);
        builder.persistence().clearStores();
        StateTransferConfigurationBuilder stateTransfer = clustering.stateTransfer().fetchInMemoryState(mode.needsStateTransfer());
        attributes = TemplateConfigurationServiceConfigurator.getAttributes(stateTransfer);
        attributes.attribute(StateTransferConfiguration.AWAIT_INITIAL_TRANSFER).reset();
        attributes.attribute(StateTransferConfiguration.TIMEOUT).reset();
    }
}
