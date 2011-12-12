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
public class DistributedCacheAdd extends ClusteredCacheAdd implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(DistributedCacheAdd.class.getPackage().getName());
    static final DistributedCacheAdd INSTANCE = new DistributedCacheAdd();

    // used to create subsystem description
    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        CacheAdd.populate(existing, operation);
        ClusteredCacheAdd.populate(existing, operation);
        populate(existing, operation) ;
        return operation;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // transfer the model data from operation to model
        populateClusteredCacheModelNode(operation, model);
        // process additional attributes
        populate(operation, model);
    }

    protected static void populate(ModelNode operation, ModelNode model) {

        if (operation.hasDefined(ModelKeys.OWNERS)) {
            model.get(ModelKeys.OWNERS).set(operation.get(ModelKeys.OWNERS)) ;
        }
        if (operation.hasDefined(ModelKeys.VIRTUAL_NODES)) {
            model.get(ModelKeys.VIRTUAL_NODES).set(operation.get(ModelKeys.VIRTUAL_NODES)) ;
        }
        if (operation.hasDefined(ModelKeys.L1_LIFESPAN)) {
            model.get(ModelKeys.L1_LIFESPAN).set(operation.get(ModelKeys.L1_LIFESPAN)) ;
        }
        // child node
        if (operation.hasDefined(ModelKeys.REHASHING)) {
            model.get(ModelKeys.REHASHING).set(operation.get(ModelKeys.REHASHING)) ;
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // use the clustered cache version
        performClusteredCacheRuntime(context, operation, model, verificationHandler, newControllers) ;
    }

    /**
     * Implementation of abstract method processModelNode suitable for distributed cache
     *
     * @param cache
     * @param configuration
     * @param additionalDeps
     * @return
     */
    @Override
    Configuration processModelNode(ModelNode cache, Configuration configuration, List<AdditionalDependency> additionalDeps) {

        // process the basic clustered configuration
        processClusteredCacheModelNode(cache, configuration, additionalDeps) ;

        // process the additional distributed attributes and elements
        FluentConfiguration fluent = configuration.fluent();
        if (cache.hasDefined(ModelKeys.OWNERS)) {
            fluent.hash().numOwners(cache.get(ModelKeys.OWNERS).asInt());
        }
        if (cache.hasDefined(ModelKeys.VIRTUAL_NODES)) {
            fluent.hash().numVirtualNodes(cache.get(ModelKeys.VIRTUAL_NODES).asInt());
        }
        if (cache.hasDefined(ModelKeys.L1_LIFESPAN)) {
            long lifespan = cache.get(ModelKeys.L1_LIFESPAN).asLong();
            if (lifespan > 0) {
                fluent.l1().lifespan(lifespan);
            } else {
                fluent.l1().disable();
            }
        }
        // process child node
        if (cache.hasDefined(ModelKeys.REHASHING)) {
            ModelNode rehashing = cache.get(ModelKeys.REHASHING);
            FluentConfiguration.HashConfig fluentHash = fluent.hash();
            if (rehashing.hasDefined(ModelKeys.ENABLED)) {
                fluentHash.rehashEnabled(rehashing.get(ModelKeys.ENABLED).asBoolean());
            }
            if (rehashing.hasDefined(ModelKeys.TIMEOUT)) {
                fluentHash.rehashRpcTimeout(rehashing.get(ModelKeys.TIMEOUT).asLong());
            }
            if (rehashing.hasDefined(ModelKeys.WAIT)) {
                fluentHash.rehashWait(rehashing.get(ModelKeys.WAIT).asLong());
            }
        }
        return configuration;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getDistributedCacheAddDescription(locale);
    }

}
