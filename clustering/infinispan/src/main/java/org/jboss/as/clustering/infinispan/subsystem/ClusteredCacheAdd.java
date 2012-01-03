package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
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

    void populateMode(ModelNode fromModel, ModelNode toModel) {
        toModel.get(ModelKeys.MODE).set(Mode.forCacheMode(CacheMode.valueOf(fromModel.get(ModelKeys.CACHE_MODE).asString())).name());
    }

    @Override
    void populateCacheMode(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        toModel.get(ModelKeys.CACHE_MODE).set(Mode.valueOf(fromModel.require(ModelKeys.MODE).asString()).apply(this.mode).name());
    }

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) {
        super.populate(fromModel, toModel);

        if (fromModel.hasDefined(ModelKeys.QUEUE_SIZE)) {
            toModel.get(ModelKeys.QUEUE_SIZE).set(fromModel.get(ModelKeys.QUEUE_SIZE));
        }
        if (fromModel.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
            toModel.get(ModelKeys.QUEUE_FLUSH_INTERVAL).set(fromModel.get(ModelKeys.QUEUE_FLUSH_INTERVAL));
        }
        if (fromModel.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
            toModel.get(ModelKeys.REMOTE_TIMEOUT).set(fromModel.get(ModelKeys.REMOTE_TIMEOUT));
        }
    }

    /**
     * Create a Configuration object initialized from the data in the operation.
     *
     * @param cache data representing cache configuration
     * @param configuration Configuration to add the data to
     * @return initialised Configuration object
     */
    @Override
    void processModelNode(ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies) {

        // process cache attributes and elements
        super.processModelNode(cache, builder, dependencies);

        // process clustered cache attributes and elements
        if (CacheMode.valueOf(cache.get(ModelKeys.CACHE_MODE).asString()).isSynchronous()) {
            if (cache.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
                builder.clustering().sync().replTimeout(cache.get(ModelKeys.REMOTE_TIMEOUT).asLong());
            }
        } else {
            if (cache.hasDefined(ModelKeys.QUEUE_SIZE)) {
                builder.clustering().async().replQueueMaxElements(cache.get(ModelKeys.QUEUE_SIZE).asInt());
            }
            if (cache.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
                builder.clustering().async().replQueueInterval(cache.get(ModelKeys.QUEUE_FLUSH_INTERVAL).asLong());
            }
        }
    }
}
