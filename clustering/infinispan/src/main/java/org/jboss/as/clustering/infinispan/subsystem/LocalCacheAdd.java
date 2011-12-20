package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.infinispan.config.Configuration.CacheMode;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 *  LocalCacheAdd handler
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LocalCacheAdd extends CacheAdd {

    static final LocalCacheAdd INSTANCE = new LocalCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode model) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        INSTANCE.populate(model, operation);
        return operation;
    }

    @Override
    void populateCacheMode(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        toModel.get(ModelKeys.CACHE_MODE).set(CacheMode.LOCAL.name());
    }
}
