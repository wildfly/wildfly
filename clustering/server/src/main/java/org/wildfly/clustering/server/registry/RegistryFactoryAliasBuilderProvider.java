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

package org.wildfly.clustering.server.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.BinaryRequirementAliasBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.server.CacheRequirementAliasBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class RegistryFactoryAliasBuilderProvider extends CacheRequirementAliasBuilderProvider {

    public RegistryFactoryAliasBuilderProvider() {
        super(ClusteringCacheRequirement.REGISTRY_FACTORY);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String aliasCacheName, String targetCacheName) {
        Collection<CapabilityServiceBuilder<?>> builders = super.getBuilders(registry, containerName, aliasCacheName, targetCacheName);
        List<CapabilityServiceBuilder<?>> result = new ArrayList<>(builders.size() + 2);
        result.addAll(builders);
        result.add(new BinaryRequirementAliasBuilder<>(registry.getServiceName(ClusteringCacheRequirement.REGISTRY), ClusteringCacheRequirement.REGISTRY, containerName, targetCacheName, ClusteringCacheRequirement.REGISTRY.getType()));
        result.add(new BinaryRequirementAliasBuilder<>(registry.getServiceName(ClusteringCacheRequirement.REGISTRY_ENTRY), ClusteringCacheRequirement.REGISTRY_ENTRY, containerName, targetCacheName, ClusteringCacheRequirement.REGISTRY_ENTRY.getType()));
        return result;
    }
}
