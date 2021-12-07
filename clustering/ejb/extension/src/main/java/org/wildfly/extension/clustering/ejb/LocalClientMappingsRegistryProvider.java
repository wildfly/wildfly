/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.ejb.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.infinispan.ClientMappingsRegistryEntryServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameRegistry;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.LocalCacheServiceConfiguratorProvider;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * A local client mappings registry provider implementation.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LocalClientMappingsRegistryProvider implements ClientMappingsRegistryProvider {
    static final Set<ClusteringCacheRequirement> REGISTRY_REQUIREMENTS = EnumSet.of(ClusteringCacheRequirement.REGISTRY, ClusteringCacheRequirement.REGISTRY_FACTORY, ClusteringCacheRequirement.GROUP);
    static final String NAME = "ejb";

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String connectorName, SupplierDependency<List<ClientMapping>> clientMappings) {
        CapabilityServiceConfigurator registryEntryConfigurator = new ClientMappingsRegistryEntryServiceConfigurator(NAME, connectorName, clientMappings);
        List<Iterable<CapabilityServiceConfigurator>> configurators = new LinkedList<>();
        configurators.add(Arrays.asList(registryEntryConfigurator));
        ServiceNameRegistry<ClusteringCacheRequirement> routingRegistry = new ServiceNameRegistry<ClusteringCacheRequirement>() {
            @Override
            public ServiceName getServiceName(ClusteringCacheRequirement requirement) {
                return REGISTRY_REQUIREMENTS.contains(requirement) ? ServiceNameFactory.parseServiceName(requirement.getName()).append(NAME, connectorName) : null;
            }
        };
        // install the underlying cache service abstractions using the configured provider
        for (CacheServiceConfiguratorProvider provider : ServiceLoader.load(LocalCacheServiceConfiguratorProvider.class, LocalCacheServiceConfiguratorProvider.class.getClassLoader())) {
            configurators.add(provider.getServiceConfigurators(routingRegistry, NAME, connectorName));
        }
        return new CompositeIterable<>(configurators);
    }
}
