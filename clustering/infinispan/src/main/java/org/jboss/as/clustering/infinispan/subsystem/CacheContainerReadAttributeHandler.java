package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
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
public class CacheContainerReadAttributeHandler implements OperationStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerReadAttributeHandler.class.getPackage().getName());

    public static final CacheContainerReadAttributeHandler INSTANCE = new CacheContainerReadAttributeHandler();

    private final ParametersValidator nameValidator = new ParametersValidator();
    private final Map<String, AttributeDefinition> attributeDefinitions ;

    private CacheContainerReadAttributeHandler() {
        this(CommonAttributes.CACHE_CONTAINER_ATTRIBUTES);
    }

    private CacheContainerReadAttributeHandler(final AttributeDefinition... definitions) {
        assert definitions != null : MESSAGES.nullVar("definitions").getLocalizedMessage();

        this.nameValidator.registerValidator(NAME, new StringLengthValidator(1));
        this.attributeDefinitions = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : definitions) {
            this.attributeDefinitions.put(def.getName(), def);
        }
    }

    /**
     * A read handler which preforms special processing for ALIAS attributes
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();

        final ModelNode submodel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(attributeName).clone();

        final AttributeDefinition attributeDefinition = getAttributeDefinition(attributeName);

        // if the attribute is ALIAS, transform from LIST to comma-delimited String
        if (attributeDefinition.getName().equals(CommonAttributes.ALIAS.getName())) {
            // convert from LIST to String (note that the value of the list ModelNode may be undefined)
            String listAsString = convertListToString(attributeDefinition.getName(), currentValue);
            context.getResult().set(listAsString);
        }
        else {
            context.getResult().set(currentValue);
        }

        // since we are not updating the model, there is no need for a RUNTIME step
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), this, CacheContainerWriteAttributeHandler.INSTANCE, flags);
        }
    }

    protected AttributeDefinition getAttributeDefinition(final String attributeName) {
        return attributeDefinitions == null ? null : attributeDefinitions.get(attributeName);
    }

    private String convertListToString(String name, ModelNode value) {

        // the model need not have any aliases defined
        if (value.getType() == ModelType.UNDEFINED)
            return "" ;

        assert (value.getType() == ModelType.LIST) : MESSAGES.validationFailed(name);
        StringBuilder result = new StringBuilder();
        List<ModelNode> list = value.asList();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            result.append(list.get(i).asString()) ;
            if (i < size-1)
                result.append(",") ;
        }
        return result.toString() ;
    }
}
