package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;

import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.FluentConfiguration;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ReplicatedCacheAdd extends ClusteredCacheAdd {

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

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) {
        super.populate(fromModel, toModel);
        // additional child node
        /*
        if (fromModel.hasDefined(ModelKeys.STATE_TRANSFER)) {
            toModel.get(ModelKeys.STATE_TRANSFER).set(fromModel.get(ModelKeys.STATE_TRANSFER)) ;
        }
        */
    }

    /**
     * Implementation of abstract method processModelNode suitable for replicated cache
     *
     * @param cache
     * @param configuration
     * @param additionalDeps
     * @return
     */
    @Override
    void processModelNode(ModelNode cache, Configuration configuration, List<AdditionalDependency<?>> additionalDeps) {
        // process the basic clustered configuration
        super.processModelNode(cache, configuration, additionalDeps);

        // process the replicated-cache attributes and elements
        FluentConfiguration fluent = configuration.fluent();

        // state transfer is a child resource
        if (cache.hasDefined(ModelKeys.SINGLETON) && cache.get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER).isDefined()) {
            ModelNode stateTransfer = cache.get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER);

            FluentConfiguration.StateRetrievalConfig fluentStateTransfer = fluent.stateRetrieval();
            if (stateTransfer.hasDefined(ModelKeys.ENABLED)) {
                fluentStateTransfer.fetchInMemoryState(stateTransfer.get(ModelKeys.ENABLED).asBoolean());
            }
            if (stateTransfer.hasDefined(ModelKeys.TIMEOUT)) {
                fluentStateTransfer.timeout(stateTransfer.get(ModelKeys.TIMEOUT).asLong());
            }
            if (stateTransfer.hasDefined(ModelKeys.FLUSH_TIMEOUT)) {
                fluentStateTransfer.logFlushTimeout(stateTransfer.get(ModelKeys.FLUSH_TIMEOUT).asLong());
            }
        }
    }
}
