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

package org.wildfly.extension.clustering.server.registry;

import java.util.List;

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.common.iteration.CompositeIterable;
import org.wildfly.extension.clustering.server.IdentityCacheRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentityRegistryFactoryServiceConfiguratorProvider extends IdentityCacheRequirementServiceConfiguratorProvider {

    public IdentityRegistryFactoryServiceConfiguratorProvider() {
        super(ClusteringCacheRequirement.REGISTRY_FACTORY);
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(OperationContext context, String containerName, String cacheName, String targetCacheName) {
        Iterable<ServiceConfigurator> configurators = super.getServiceConfigurators(context, containerName, cacheName, targetCacheName);
        ServiceName registryServiceName = ClusteringCacheRequirement.REGISTRY.getServiceName(context, containerName, cacheName);
        ServiceName registryEntryServiceName = ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(context, containerName, cacheName);

        ServiceConfigurator registryConfigurator = new IdentityCapabilityServiceConfigurator<>(registryServiceName, ClusteringCacheRequirement.REGISTRY, containerName, targetCacheName).configure(context);
        ServiceConfigurator registryEntryConfigurator = new IdentityCapabilityServiceConfigurator<>(registryEntryServiceName, ClusteringCacheRequirement.REGISTRY_ENTRY, containerName, targetCacheName).configure(context);
        return new CompositeIterable<>(configurators, List.of(registryConfigurator, registryEntryConfigurator));
    }
}
