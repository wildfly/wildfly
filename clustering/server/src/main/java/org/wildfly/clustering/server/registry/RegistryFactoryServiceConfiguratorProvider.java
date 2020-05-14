/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.registry;

import java.util.Collections;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.CacheJndiNameFactory;
import org.wildfly.clustering.server.CacheRequirementServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Provides the requisite builders for a clustered {@link RegistryFactory} created from the specified factory.
 * @author Paul Ferraro
 */
public class RegistryFactoryServiceConfiguratorProvider extends CacheRequirementServiceConfiguratorProvider<RegistryFactory<Object, Object>> {

    protected RegistryFactoryServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<RegistryFactory<Object, Object>> factory) {
        super(ClusteringCacheRequirement.REGISTRY_FACTORY, factory, CacheJndiNameFactory.REGISTRY_FACTORY);
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String cacheName) {
        Iterable<CapabilityServiceConfigurator> configurators = super.getServiceConfigurators(registry, containerName, cacheName);
        ServiceName registryServiceName = registry.getServiceName(ClusteringCacheRequirement.REGISTRY);
        if (registryServiceName == null) return configurators;

        CapabilityServiceConfigurator configurator = new RegistryServiceConfigurator<>(registryServiceName, containerName, cacheName);
        return new CompositeIterable<>(configurators, Collections.singleton(configurator));
    }
}
