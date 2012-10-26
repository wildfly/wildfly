package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreResource extends BaseStoreResource {

    private static final PathElement STORE_PATH = PathElement.pathElement(ModelKeys.STORE, ModelKeys.STORE_NAME);

    // attributes
    static final SimpleAttributeDefinition CLASS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLASS, ModelType.STRING, false)
                    .setXmlName(Attribute.CLASS.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final AttributeDefinition[] STORE_ATTRIBUTES = {CLASS};

    // operations
    private static final OperationDefinition STORE_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ADD, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STORE))
        .setParameters(COMMON_STORE_PARAMETERS)
        .addParameter(CLASS)
        .setAttributeResolver(InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STORE))
        .build();

    public StoreResource() {
        super(STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STORE),
                CacheConfigOperationHandlers.STORE_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(STORE_ATTRIBUTES);
        for (AttributeDefinition attr : STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    // override the add operation to provide a custom definition (for the optional PROPERTIES parameter to add())
    @Override
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(STORE_ADD_DEFINITION, handler);
    }
}
