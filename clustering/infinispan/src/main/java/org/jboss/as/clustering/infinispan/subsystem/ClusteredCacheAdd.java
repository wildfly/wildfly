package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for clustered cache add operations
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class ClusteredCacheAdd extends CacheAdd {

    // the attribute definition for the cache mode
    public static SimpleAttributeDefinition MODE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, false)
                    .setXmlName(Attribute.MODE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<Mode>(Mode.class, true, false))
                    .build();

    ClusteredCacheAdd(CacheMode mode) {
        super(mode);
    }

    // used in createOperation only
    void populateMode(ModelNode fromModel, ModelNode toModel) {
        toModel.get(ModelKeys.MODE).set(Mode.forCacheMode(CacheMode.valueOf(fromModel.get(ModelKeys.MODE).asString())).name());
    }

    @Override
    void populateCacheMode(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        toModel.get(ModelKeys.MODE).set(Mode.valueOf(fromModel.require(ModelKeys.MODE).asString()).apply(this.mode).name());
    }

    @Override
    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        super.populate(fromModel, toModel);

        CommonAttributes.ASYNC_MARSHALLING.validateAndSet(fromModel, toModel);
        CommonAttributes.QUEUE_SIZE.validateAndSet(fromModel, toModel);
        CommonAttributes.QUEUE_FLUSH_INTERVAL.validateAndSet(fromModel, toModel);
        CommonAttributes.REMOTE_TIMEOUT.validateAndSet(fromModel, toModel);
    }

    /**
     * Create a Configuration object initialized from the data in the operation.
     *
     * @param cache data representing cache configuration
     * @param builder
     * @param dependencies
     *
     * @return initialised Configuration object
     */
    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies)
            throws OperationFailedException{

        // process cache attributes and elements
        super.processModelNode(context, containerName, cache, builder, dependencies);

        final long remoteTimeout = CommonAttributes.REMOTE_TIMEOUT.resolveModelAttribute(context, cache).asLong();
        final int queueSize = CommonAttributes.QUEUE_SIZE.resolveModelAttribute(context, cache).asInt();
        final long queueFlushInterval = CommonAttributes.QUEUE_FLUSH_INTERVAL.resolveModelAttribute(context, cache).asLong();
        final boolean asyncMarshalling = CommonAttributes.ASYNC_MARSHALLING.resolveModelAttribute(context, cache).asBoolean();

        // process clustered cache attributes and elements
        if (CacheMode.valueOf(cache.get(ModelKeys.MODE).asString()).isSynchronous()) {
            builder.clustering().sync().replTimeout(remoteTimeout);
        } else {
            builder.clustering().async().replQueueMaxElements(queueSize);
            builder.clustering().async().replQueueInterval(queueFlushInterval);
            if(asyncMarshalling)
                builder.clustering().async().asyncMarshalling();
            else
                builder.clustering().async().syncMarshalling();
        }
    }
}
