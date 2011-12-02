package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;

import org.infinispan.config.Configuration;
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
public class InvalidationCacheAdd extends ClusteredCacheAdd implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(InvalidationCacheAdd.class.getPackage().getName()) ;
    static final InvalidationCacheAdd INSTANCE = new InvalidationCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        CacheAdd.populate(existing, operation);
        ClusteredCacheAdd.populate(existing, operation);
        return operation;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // transfer the model data from operation to model
        populateClusteredCacheModelNode(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // use the clustered cache version
        performClusteredCacheRuntime(context, operation, model, verificationHandler, newControllers) ;
    }

    /**
     * Implementation of abstract method processModelNode suitable for replicated cache
     *
     * @param model
     * @param overrides
     * @param additionalDeps
     * @return
     */
    protected Configuration processModelNode(ModelNode model, Configuration overrides, List<AdditionalDependency> additionalDeps) {
       // basic clustered cache model node processing
       return processClusteredCacheModelNode(model, overrides, additionalDeps);
    }

    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getInvalidationCacheAddDescription(locale);
    }

}
