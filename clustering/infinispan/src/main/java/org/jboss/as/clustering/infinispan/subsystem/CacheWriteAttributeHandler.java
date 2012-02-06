package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.CACHE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.CLUSTERED_CACHE_ATTRIBUTES;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.DISTRIBUTED_CACHE_ATTRIBUTES;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Attribute handler for cache resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheWriteAttributeHandler implements OperationStepHandler, SelfRegisteringAttributeHandler {

    /** The basic and clustered cache attributes operation handler. */
    static final SelfRegisteringAttributeHandler CACHE_ATTR = new CacheWriteAttributeHandler(CACHE_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler CLUSTERED_CACHE_ATTR = new CacheWriteAttributeHandler(CLUSTERED_CACHE_ATTRIBUTES);
    static final SelfRegisteringAttributeHandler DISTRIBUTED_CACHE_ATTR = new CacheWriteAttributeHandler(DISTRIBUTED_CACHE_ATTRIBUTES);

    public static final CacheWriteAttributeHandler INSTANCE = new CacheWriteAttributeHandler();
    private final ParametersValidator nameValidator = new ParametersValidator();

    private final Map<String, AttributeDefinition> attributeDefinitions;

    private CacheWriteAttributeHandler(final AttributeDefinition... definitions) {
        assert definitions != null : MESSAGES.nullVar("definitions").getLocalizedMessage();
        attributeDefinitions = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : definitions) {
            attributeDefinitions.put(def.getName(), def);
        }
    }

    /**
     * An attribute write handler which performs special processing for ALIAS attributes.
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws org.jboss.as.controller.OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();

        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value
        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();

        AttributeDefinition attributeDefinition = null;

        // special processing for MODE
        if (attributeName.equals(ModelKeys.MODE)) {
            // 1. Validate the inputs if possible
            // 2. Get the current cache mode
            // 3. set the model value

            attributeDefinition = getAttributeDefinition(attributeName);
            if (attributeDefinition != null) {
                final ModelNode syntheticOp = new ModelNode();
                syntheticOp.get(attributeName).set(newValue);
                attributeDefinition.validateOperation(syntheticOp);
            }
            CacheMode mode = getCacheMode(operation) ;
            submodel.get(ModelKeys.MODE).set(Mode.valueOf(newValue.asString()).apply(mode).name());
        }
        else {
            attributeDefinition = getAttributeDefinition(attributeName);
            if (attributeDefinition != null) {
                final ModelNode syntheticOp = new ModelNode();
                syntheticOp.get(attributeName).set(newValue);
                attributeDefinition.validateAndSet(syntheticOp, submodel);
            } else {
                submodel.get(attributeName).set(newValue);
            }
        }

        // since we modified the model, set reload required
        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.reloadRequired();
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.revertReloadRequired();
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

     /**
      * Gets whether a {@link org.jboss.as.controller.OperationContext.Stage#RUNTIME} handler should be added. This default implementation
      * returns {@code true} if the {@link org.jboss.as.controller.OperationContext#getType() context type} is {@link org.jboss.as.controller.OperationContext.Type#SERVER}
      * and {@link org.jboss.as.controller.OperationContext#isBooting() context.isBooting()} returns {@code false}.
      *
      * @param context operation context
      * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
      */
     protected boolean requiresRuntime(OperationContext context) {
         return context.getProcessType().isServer() && !context.isBooting();
     }

     protected AttributeDefinition getAttributeDefinition(final String attributeName) {
         return attributeDefinitions == null ? null : attributeDefinitions.get(attributeName);
     }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        for (AttributeDefinition attr : attributeDefinitions.values()) {
           registry.registerReadWriteAttribute(attr.getName(), CacheReadAttributeHandler.INSTANCE, this, flags);
        }
    }

    /*
     * The operation address is of the form /subsystem=infinispan/cache-container=X/cache-type=Y:write-attribute()
     */
    public static CacheMode getCacheMode(ModelNode operation) {

        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String cacheType = cacheAddress.getLastElement().getKey();

        CacheMode mode = null;
        if (cacheType.equals(ModelKeys.LOCAL_CACHE)) {
            mode = CacheMode.LOCAL;
        } else if (cacheType.equals(ModelKeys.INVALIDATION_CACHE)) {
            mode = CacheMode.INVALIDATION_SYNC;
        }
        else if (cacheType.equals(ModelKeys.REPLICATED_CACHE)) {
            mode = CacheMode.REPL_SYNC;
        }
        else if (cacheType.equals(ModelKeys.DISTRIBUTED_CACHE)) {
            mode = CacheMode.DIST_SYNC;
        }
        return mode;
    }

}
