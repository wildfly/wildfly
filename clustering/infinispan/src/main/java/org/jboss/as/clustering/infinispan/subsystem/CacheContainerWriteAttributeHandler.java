package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

/**
 * Attribute handler for cache-container resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheContainerWriteAttributeHandler implements OperationStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerWriteAttributeHandler.class.getPackage().getName());

    public static final CacheContainerWriteAttributeHandler INSTANCE = new CacheContainerWriteAttributeHandler();
    private final ParametersValidator nameValidator = new ParametersValidator();

    private final Map<String, AttributeDefinition> attributeDefinitions;

    private CacheContainerWriteAttributeHandler() {
        this(CommonAttributes.CACHE_CONTAINER_ATTRIBUTES);
    }

    private CacheContainerWriteAttributeHandler(final AttributeDefinition... definitions) {
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
     * @throws OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value
        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(attributeName).clone();

        // for the attribute ALIAS, add the aliases in newValue to the current value
        if (attributeName.equals(ModelKeys.ALIAS)) {
            newValue = createAliasList(newValue.asString()) ;
        }

        final AttributeDefinition attributeDefinition = getAttributeDefinition(attributeName);
        if (attributeDefinition != null) {
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            attributeDefinition.validateAndSet(syntheticOp, submodel);
        } else {
            submodel.get(attributeName).set(newValue);
        }

        // since we modified the model, set reload required
        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.reloadRequired();
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }


     /**
      * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added. This default implementation
      * returns {@code true} if the {@link OperationContext#getType() context type} is {@link OperationContext.Type#SERVER}
      * and {@link OperationContext#isBooting() context.isBooting()} returns {@code false}.
      *
      * @param context operation context
      * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
      */
     protected boolean requiresRuntime(OperationContext context) {
         return context.getType() == OperationContext.Type.SERVER && !context.isBooting();
     }

     protected AttributeDefinition getAttributeDefinition(final String attributeName) {
         return attributeDefinitions == null ? null : attributeDefinitions.get(attributeName);
     }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), CacheContainerReadAttributeHandler.INSTANCE, this, flags);
        }
    }

    /**
     * Creates a new ModelNode representing the list of aliases.
     *
     * @param aliasString comma-delimited list of new aliases to add
     * @return LIST ModelNode with the added aliases
     */
    private ModelNode createAliasList(String aliasString) {

        // check for empty string
        if (aliasString == null || aliasString.equals(""))
            return new ModelNode() ;

        final String ALIAS_SEPARATOR = ",";
        ModelNode newList = new ModelNode() ;
        String[] aliases = aliasString.split(ALIAS_SEPARATOR) ;

        for (String alias : aliases) {
            newList.add().set(alias) ;
        }
        return newList ;
    }
}
