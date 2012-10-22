package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
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
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/state-transfer=STATE_TRANSFER
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StateTransferResource extends SimpleResourceDefinition {

    private static final PathElement STATE_TRANSFER_PATH = PathElement.pathElement(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
    public static final StateTransferResource INSTANCE = new StateTransferResource();

    // attributes
    static final SimpleAttributeDefinition CHUNK_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CHUNK_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.CHUNK_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(10000))
                    .build();

    // enabled (used in state transfer, rehashing)
    static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ENABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.ENABLED.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();

    // timeout (used in state transfer, rehashing)
    static final SimpleAttributeDefinition TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(60000))
                    .build();

    static final AttributeDefinition[] STATE_TRANSFER_ATTRIBUTES = {ENABLED, TIMEOUT, CHUNK_SIZE};

    public StateTransferResource() {
        super(STATE_TRANSFER_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STATE_TRANSFER),
                CacheConfigOperationHandlers.STATE_TRANSFER_ADD,
                CacheConfigOperationHandlers.REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(STATE_TRANSFER_ATTRIBUTES);
        for (AttributeDefinition attr : STATE_TRANSFER_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
