package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/expiration=EXPIRATION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ExpirationResource extends SimpleResourceDefinition {

    private static final PathElement EXPIRATION_PATH = PathElement.pathElement(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);

    // attributes
    static final SimpleAttributeDefinition INTERVAL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INTERVAL, ModelType.LONG, true)
                    .setXmlName(Attribute.INTERVAL.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(60000))
                    .build();

    static final SimpleAttributeDefinition LIFESPAN =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LIFESPAN, ModelType.LONG, true)
                    .setXmlName(Attribute.LIFESPAN.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(-1))
                    .build();

    static final SimpleAttributeDefinition MAX_IDLE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MAX_IDLE, ModelType.LONG, true)
                    .setXmlName(Attribute.MAX_IDLE.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(-1))
                    .build();

    static final AttributeDefinition[] EXPIRATION_ATTRIBUTES = {MAX_IDLE, LIFESPAN, INTERVAL};

    public ExpirationResource() {
        super(EXPIRATION_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.EXPIRATION),
                CacheConfigOperationHandlers.EXPIRATION_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(EXPIRATION_ATTRIBUTES);
        for (AttributeDefinition attr : EXPIRATION_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
