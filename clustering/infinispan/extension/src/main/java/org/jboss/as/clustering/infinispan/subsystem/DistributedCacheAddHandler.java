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

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class DistributedCacheAddHandler extends SharedStateCacheAddHandler {

    DistributedCacheAddHandler() {
        super(CacheMode.DIST_SYNC);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);

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
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, AdvancedCacheConfigurationBuilder configBuilder) throws OperationFailedException {

        // process the basic clustered configuration
        super.processModelNode(context, containerName, containerModel, cache, configBuilder);

        ConfigurationBuilder builder = configBuilder.getConfigurationBuilder();

        configBuilder.setConsistentHashStrategy(ConsistentHashStrategy.valueOf(DistributedCacheResourceDefinition.CONSISTENT_HASH_STRATEGY.resolveModelAttribute(context, cache).asString()));

        builder.clustering().hash()
            .numOwners(DistributedCacheResourceDefinition.OWNERS.resolveModelAttribute(context, cache).asInt())
            .numSegments(DistributedCacheResourceDefinition.SEGMENTS.resolveModelAttribute(context, cache).asInt())
            .capacityFactor(ModelNodes.asFloat(DistributedCacheResourceDefinition.CAPACITY_FACTOR.resolveModelAttribute(context, cache)))
        ;

        long lifespan = DistributedCacheResourceDefinition.L1_LIFESPAN.resolveModelAttribute(context, cache).asLong();
        if (lifespan > 0) {
            builder.clustering().l1().enable().lifespan(lifespan);
        } else {
            builder.clustering().l1().disable();
        }
    }
}
