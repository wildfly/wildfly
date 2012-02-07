package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InvalidationCacheAdd extends ClusteredCacheAdd {

    static final InvalidationCacheAdd INSTANCE = new InvalidationCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode model) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        INSTANCE.populateMode(model, operation);
        INSTANCE.populate(model, operation);
        return operation;
    }

    private InvalidationCacheAdd() {
        super(CacheMode.INVALIDATION_SYNC);
    }
}
