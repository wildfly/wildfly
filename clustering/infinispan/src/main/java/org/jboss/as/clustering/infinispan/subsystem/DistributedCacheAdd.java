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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class DistributedCacheAdd extends SharedStateCacheAdd {

    static final DistributedCacheAdd INSTANCE = new DistributedCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode model) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        INSTANCE.populate(model, operation);
        return operation;
    }

    private DistributedCacheAdd() {
        super(CacheMode.DIST_SYNC);
    }

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        super.populate(fromModel, toModel);

        @SuppressWarnings("deprecation")
        final String deprecatedKey = ModelKeys.VIRTUAL_NODES;
        if (fromModel.hasDefined(deprecatedKey)
                && fromModel.get(deprecatedKey).asInt() != 1) {
            // log a WARN
            InfinispanLogger.ROOT_LOGGER.virtualNodesAttributeDeprecated();
            // convert the virtual-nodes value to segments and update the incoming model
            // TBD: what to do it both values are coded?
            ModelNode convertedValue = SegmentsAndVirtualNodeConverter.virtualNodesToSegments(fromModel.get(deprecatedKey));
            fromModel.get(ModelKeys.SEGMENTS).set(convertedValue);
            fromModel.remove(deprecatedKey);
        }

        DistributedCacheResource.OWNERS.validateAndSet(fromModel, toModel);
        DistributedCacheResource.SEGMENTS.validateAndSet(fromModel, toModel);
        DistributedCacheResource.L1_LIFESPAN.validateAndSet(fromModel, toModel);
    }

    /**
     * Implementation of abstract method processModelNode suitable for distributed cache
     *
     * @param context
     * @param containerName
     * @param builder
     * @param dependencies
     * @return
     */
    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies)
            throws OperationFailedException {

        // process the basic clustered configuration
        super.processModelNode(context, containerName, cache, builder, dependencies);

        final int owners = DistributedCacheResource.OWNERS.resolveModelAttribute(context, cache).asInt();
        final int segments = DistributedCacheResource.SEGMENTS.resolveModelAttribute(context, cache).asInt();
        final long lifespan = DistributedCacheResource.L1_LIFESPAN.resolveModelAttribute(context, cache).asLong();

        // process the additional distributed attributes and elements
        builder.clustering().hash()
            .numOwners(owners)
            .numSegments(segments)
        ;
        if (lifespan > 0) {
            builder.clustering().l1().lifespan(lifespan);
        } else {
            builder.clustering().l1().disable();
        }
    }
}
