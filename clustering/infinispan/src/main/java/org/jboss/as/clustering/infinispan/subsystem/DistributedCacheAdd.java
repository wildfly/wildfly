package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;

import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.FluentConfiguration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class DistributedCacheAdd extends ClusteredCacheAdd implements DescriptionProvider {

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
        // child node
        if (fromModel.hasDefined(ModelKeys.REHASHING)) {
            toModel.get(ModelKeys.REHASHING).set(fromModel.get(ModelKeys.REHASHING));
        }
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
    void processModelNode(ModelNode cache, Configuration configuration, List<AdditionalDependency<?>> additionalDeps) {
        // process the basic clustered configuration
        super.processModelNode(cache, configuration, additionalDeps);

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
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getDistributedCacheAddDescription(locale);
    }
}
