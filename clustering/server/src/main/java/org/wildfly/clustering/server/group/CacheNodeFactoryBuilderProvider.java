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
package org.wildfly.clustering.server.group;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.DistributedCacheBuilderProvider;
import org.wildfly.clustering.spi.LocalCacheBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * Provides the requisite builders for both local and clustered service implementations of a {@link org.wildfly.clustering.group.NodeFactory} for a cache.
 * @author Paul Ferraro
 */
public class CacheNodeFactoryBuilderProvider implements LocalCacheBuilderProvider, DistributedCacheBuilderProvider {

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String cacheName) {
        return Collections.singleton(new CacheNodeFactoryBuilder(registry, containerName));
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
