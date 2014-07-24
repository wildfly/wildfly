/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class DistributedCacheAddHandler extends SharedStateCacheAddHandler {

    static final DistributedCacheAddHandler INSTANCE = new DistributedCacheAddHandler();

    private DistributedCacheAddHandler() {
        super(CacheMode.DIST_SYNC);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);

        @SuppressWarnings("deprecation")
        final String deprecatedKey = ModelKeys.VIRTUAL_NODES;
        if (operation.hasDefined(deprecatedKey)
                && operation.get(deprecatedKey).asInt() != 1) {
            // log a WARN
            InfinispanLogger.ROOT_LOGGER.virtualNodesAttributeDeprecated();
            // convert the virtual-nodes value to segments and update the incoming model
            // TBD: what to do it both values are coded?
            ModelNode convertedValue = SegmentsAndVirtualNodeConverter.virtualNodesToSegments(operation.get(deprecatedKey));
            operation.get(ModelKeys.SEGMENTS).set(convertedValue);
            operation.remove(deprecatedKey);
        }

        for (AttributeDefinition attribute: DistributedCacheResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    /**
     * Implementation of abstract method processModelNode suitable for distributed cache.
     *
     * {@inheritDoc}
     */
    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, ConfigurationBuilder builder, CacheConfigurationDependencies cacheConfigurationDependencies, CacheDependencies cacheDependencies, List<Dependency<?>> dependencies) throws OperationFailedException {

        // process the basic clustered configuration
        super.processModelNode(context, containerName, containerModel, cache, builder, cacheConfigurationDependencies, cacheDependencies, dependencies);

        int owners = DistributedCacheResourceDefinition.OWNERS.resolveModelAttribute(context, cache).asInt();
        int segments = DistributedCacheResourceDefinition.SEGMENTS.resolveModelAttribute(context, cache).asInt();
        long lifespan = DistributedCacheResourceDefinition.L1_LIFESPAN.resolveModelAttribute(context, cache).asLong();
        Double capacityFactor = DistributedCacheResourceDefinition.CAPACITY_FACTOR.resolveModelAttribute(context, cache).asDouble();
        ConsistentHashStrategy strategy = ConsistentHashStrategy.valueOf(DistributedCacheResourceDefinition.CONSISTENT_HASH_STRATEGY.resolveModelAttribute(context, cache).asString());

        cacheConfigurationDependencies.setConsistentHashStrategy(strategy);

        builder.clustering().hash()
            .numOwners(owners)
            .numSegments(segments)
            .capacityFactor(capacityFactor.floatValue())
        ;
        if (lifespan > 0) {
            builder.clustering().l1().lifespan(lifespan);
        } else {
            builder.clustering().l1().disable();
        }
    }
}
