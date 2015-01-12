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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Base class for clustered cache add operations
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class ClusteredCacheAddHandler extends CacheAddHandler {


    ClusteredCacheAddHandler(CacheMode mode) {
        super(mode);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);

        for (AttributeDefinition attribute: ClusteredCacheResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    /**
     * Configure builder from the data in the operation.
     *
     * {@inheritDoc}
     */
    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, AdvancedCacheConfigurationBuilder configBuilder) throws OperationFailedException {

        // process cache attributes and elements
        super.processModelNode(context, containerName, containerModel, cache, configBuilder);

        ConfigurationBuilder builder = configBuilder.getConfigurationBuilder();

        // required attribute MODE (ASYNC/SYNC)
        final Mode mode = Mode.valueOf(ClusteredCacheResourceDefinition.MODE.resolveModelAttribute(context, cache).asString());

        final int queueSize = ClusteredCacheResourceDefinition.QUEUE_SIZE.resolveModelAttribute(context, cache).asInt();

        // adjust the cache mode used based on the value of clustered attribute MODE
        CacheMode cacheMode = mode.apply(this.mode);
        builder.clustering().cacheMode(cacheMode);

        // process clustered cache attributes and elements
        if (cacheMode.isSynchronous()) {
            builder.clustering().sync().replTimeout(ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.resolveModelAttribute(context, cache).asLong());
        } else {
            builder.clustering().async()
                    .useReplQueue(queueSize > 0)
                    .replQueueMaxElements(queueSize)
                    .replQueueInterval(ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.resolveModelAttribute(context, cache).asLong())
            ;
            if (ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.resolveModelAttribute(context, cache).asBoolean()) {
                builder.clustering().async().asyncMarshalling();
            } else {
                builder.clustering().async().syncMarshalling();
            }
        }
    }
}
