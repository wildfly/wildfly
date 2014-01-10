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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Base class for clustered cache add operations
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class ClusteredCacheAdd extends CacheAdd {


    ClusteredCacheAdd(CacheMode mode) {
        super(mode);
    }

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        super.populate(fromModel, toModel);

        ClusteredCacheResourceDefinition.MODE.validateAndSet(fromModel, toModel);
        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.validateAndSet(fromModel, toModel);
        ClusteredCacheResourceDefinition.QUEUE_SIZE.validateAndSet(fromModel, toModel);
        ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.validateAndSet(fromModel, toModel);
        ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.validateAndSet(fromModel, toModel);
    }

    /**
     * Create a Configuration object initialized from the data in the operation.
     *
     * @param cache data representing cache configuration
     * @param builder
     * @param dependencies
     *
     * @return initialised Configuration object
     */
    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode cache, ConfigurationBuilder builder, CacheConfigurationDependencies cacheConfigurationDependencies, CacheDependencies cacheDependencies, List<Dependency<?>> dependencies) throws OperationFailedException {

        // process cache attributes and elements
        super.processModelNode(context, containerName, cache, builder, cacheConfigurationDependencies, cacheDependencies, dependencies);

        // required attribute MODE (ASYNC/SYNC)
        final Mode mode = Mode.valueOf(ClusteredCacheResourceDefinition.MODE.resolveModelAttribute(context, cache).asString()) ;

        final long remoteTimeout = ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.resolveModelAttribute(context, cache).asLong();
        final int queueSize = ClusteredCacheResourceDefinition.QUEUE_SIZE.resolveModelAttribute(context, cache).asInt();
        final long queueFlushInterval = ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.resolveModelAttribute(context, cache).asLong();
        final boolean asyncMarshalling = ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.resolveModelAttribute(context, cache).asBoolean();

        // adjust the cache mode used based on the value of clustered attribute MODE
        CacheMode cacheMode = mode.apply(this.mode);
        builder.clustering().cacheMode(cacheMode);

        // process clustered cache attributes and elements
        if (cacheMode.isSynchronous()) {
            builder.clustering().sync().replTimeout(remoteTimeout);
        } else {
            builder.clustering().async().useReplQueue(queueSize > 0);
            builder.clustering().async().replQueueMaxElements(queueSize);
            builder.clustering().async().replQueueInterval(queueFlushInterval);
            if(asyncMarshalling)
                builder.clustering().async().asyncMarshalling();
            else
                builder.clustering().async().syncMarshalling();
        }
    }
}
