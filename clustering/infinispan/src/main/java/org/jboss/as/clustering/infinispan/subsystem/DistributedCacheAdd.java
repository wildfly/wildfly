package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class DistributedCacheAdd extends SharedStateCacheAdd {

    static final DistributedCacheAdd INSTANCE = new DistributedCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode model) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        INSTANCE.populateMode(model, operation);
        INSTANCE.populate(model, operation);
        return operation;
    }

    private DistributedCacheAdd() {
        super(CacheMode.DIST_SYNC);
    }

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) {
        super.populate(fromModel, toModel);

        if (fromModel.hasDefined(ModelKeys.OWNERS)) {
            toModel.get(ModelKeys.OWNERS).set(fromModel.get(ModelKeys.OWNERS));
        }
        if (fromModel.hasDefined(ModelKeys.VIRTUAL_NODES)) {
            toModel.get(ModelKeys.VIRTUAL_NODES).set(fromModel.get(ModelKeys.VIRTUAL_NODES));
        }
        if (fromModel.hasDefined(ModelKeys.L1_LIFESPAN)) {
            toModel.get(ModelKeys.L1_LIFESPAN).set(fromModel.get(ModelKeys.L1_LIFESPAN));
        }
    }

    /**
     * Implementation of abstract method processModelNode suitable for distributed cache
     *
     * @param cache
     * @param configuration
     * @param dependencies
     * @return
     */
    @Override
    void processModelNode(ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies) {
        // process the basic clustered configuration
        super.processModelNode(cache, builder, dependencies);

        // process the additional distributed attributes and elements
        if (cache.hasDefined(ModelKeys.OWNERS)) {
            builder.clustering().hash().numOwners(cache.get(ModelKeys.OWNERS).asInt());
        }
        if (cache.hasDefined(ModelKeys.VIRTUAL_NODES)) {
            builder.clustering().hash().numVirtualNodes(cache.get(ModelKeys.VIRTUAL_NODES).asInt());
        }
        if (cache.hasDefined(ModelKeys.L1_LIFESPAN)) {
            long lifespan = cache.get(ModelKeys.L1_LIFESPAN).asLong();
            if (lifespan > 0) {
                builder.clustering().l1().lifespan(lifespan);
            } else {
                builder.clustering().l1().disable();
            }
        }
    }
}
