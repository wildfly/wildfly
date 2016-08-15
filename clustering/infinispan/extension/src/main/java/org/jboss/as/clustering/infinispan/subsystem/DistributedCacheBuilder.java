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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.DistributedCacheResourceDefinition.Attribute.*;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.cache.HashConfigurationBuilder;
import org.infinispan.configuration.cache.L1Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory;
import org.infinispan.distribution.ch.impl.TopologyAwareConsistentHashFactory;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * Builds the configuration for a distributed cache.
 * @author Paul Ferraro
 */
public class DistributedCacheBuilder extends SharedStateCacheBuilder {

    private final InjectedValue<GlobalConfiguration> container = new InjectedValue<>();
    private final String containerName;

    private volatile HashConfiguration hash;
    private volatile L1Configuration l1;
    private volatile ConsistentHashStrategy consistentHashStrategy;

    DistributedCacheBuilder(String containerName, String cacheName) {
        super(containerName, cacheName, CacheMode.DIST_SYNC);
        this.containerName = containerName;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return super.build(target).addDependency(CacheContainerServiceName.CONFIGURATION.getServiceName(this.containerName), GlobalConfiguration.class, this.container);
    }

    @Override
    public Builder<Configuration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.consistentHashStrategy = ModelNodes.asEnum(CONSISTENT_HASH_STRATEGY.getDefinition().resolveModelAttribute(context, model), ConsistentHashStrategy.class);

        ClusteringConfigurationBuilder builder = new ConfigurationBuilder().clustering();

        this.hash = builder.hash()
                .capacityFactor(CAPACITY_FACTOR.getDefinition().resolveModelAttribute(context, model).asInt())
                .numOwners(OWNERS.getDefinition().resolveModelAttribute(context, model).asInt())
                .numSegments(SEGMENTS.getDefinition().resolveModelAttribute(context, model).asInt())
                .create();

        long l1Lifespan = L1_LIFESPAN.getDefinition().resolveModelAttribute(context, model).asLong();
        this.l1 = builder.l1().enabled(l1Lifespan > 0).lifespan(l1Lifespan).create();

        return super.configure(context, model);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        HashConfigurationBuilder hash = builder.clustering()
                .l1().read(this.l1)
                .hash().read(this.hash);

        // ConsistentHashStrategy.INTER_CACHE is Infinispan's default behavior
        if (this.consistentHashStrategy == ConsistentHashStrategy.INTRA_CACHE) {
            hash.consistentHashFactory(this.container.getValue().transport().hasTopologyInfo() ? new TopologyAwareConsistentHashFactory() : new DefaultConsistentHashFactory());
        }
    }
}
