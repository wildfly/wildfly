package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/eviction=EVICTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StorePropertyResource extends SimpleResourceDefinition {

    private static final PathElement STORE_PROPERTY_PATH = PathElement.pathElement(ModelKeys.PROPERTY);

    // attributes
    static final SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder("value", ModelType.STRING, false)
                    .setXmlName("value")
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public StorePropertyResource() {
        super(STORE_PROPERTY_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.PROPERTY),
                CacheConfigOperationHandlers.STORE_PROPERTY_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // do we need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(VALUE);
        resourceRegistration.registerReadWriteAttribute(VALUE, null, writeHandler);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
