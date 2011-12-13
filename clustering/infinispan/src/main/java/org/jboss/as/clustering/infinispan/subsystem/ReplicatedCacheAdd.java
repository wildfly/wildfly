package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ReplicatedCacheAdd extends ClusteredCacheAdd implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(ReplicatedCacheAdd.class.getPackage().getName());
    static final ReplicatedCacheAdd INSTANCE = new ReplicatedCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        CacheAdd.populate(existing, operation);
        ClusteredCacheAdd.populate(existing, operation);
        populate(existing, operation);
        return operation;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // transfer the model data from operation to model
        populateClusteredCacheModelNode(operation, model);
        populate(operation, model);
    }

    protected static void populate(ModelNode operation, ModelNode model) {
        // additional child node
        if (operation.hasDefined(ModelKeys.STATE_TRANSFER)) {
            model.get(ModelKeys.STATE_TRANSFER).set(operation.get(ModelKeys.STATE_TRANSFER)) ;
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // use the clustered cache version
        performClusteredCacheRuntime(context, operation, model, verificationHandler, newControllers) ;
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
    Configuration processModelNode(ModelNode cache, Configuration configuration, List<AdditionalDependency> additionalDeps) {
        // process the basic clustered configuration
        processClusteredCacheModelNode(cache, configuration, additionalDeps);

        // process the replicated-cache attributes and elements
        FluentConfiguration fluent = configuration.fluent();
        if (cache.hasDefined(ModelKeys.STATE_TRANSFER)) {
            ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER) ;
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
        return configuration;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getReplicatedCacheAddDescription(locale);
    }
}
