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

        ClusteredCacheResource.MODE.validateAndSet(fromModel, toModel);
        ClusteredCacheResource.ASYNC_MARSHALLING.validateAndSet(fromModel, toModel);
        ClusteredCacheResource.QUEUE_SIZE.validateAndSet(fromModel, toModel);
        ClusteredCacheResource.QUEUE_FLUSH_INTERVAL.validateAndSet(fromModel, toModel);
        ClusteredCacheResource.REMOTE_TIMEOUT.validateAndSet(fromModel, toModel);
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
    void processModelNode(OperationContext context, String containerName, ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies)
            throws OperationFailedException{

        // process cache attributes and elements
        super.processModelNode(context, containerName, cache, builder, dependencies);

        // required attribute MODE (ASYNC/SYNC)
        final Mode mode = Mode.valueOf(ClusteredCacheResource.MODE.resolveModelAttribute(context, cache).asString()) ;

        final long remoteTimeout = ClusteredCacheResource.REMOTE_TIMEOUT.resolveModelAttribute(context, cache).asLong();
        final int queueSize = ClusteredCacheResource.QUEUE_SIZE.resolveModelAttribute(context, cache).asInt();
        final long queueFlushInterval = ClusteredCacheResource.QUEUE_FLUSH_INTERVAL.resolveModelAttribute(context, cache).asLong();
        final boolean asyncMarshalling = ClusteredCacheResource.ASYNC_MARSHALLING.resolveModelAttribute(context, cache).asBoolean();

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
