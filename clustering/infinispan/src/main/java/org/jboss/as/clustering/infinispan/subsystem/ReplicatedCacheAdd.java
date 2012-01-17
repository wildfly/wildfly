package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ReplicatedCacheAdd extends SharedStateCacheAdd {

    static final ReplicatedCacheAdd INSTANCE = new ReplicatedCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode model) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        INSTANCE.populateMode(model, operation);
        INSTANCE.populate(model, operation);
        return operation;
    }

    private ReplicatedCacheAdd() {
        super(CacheMode.REPL_SYNC);
    }
}
