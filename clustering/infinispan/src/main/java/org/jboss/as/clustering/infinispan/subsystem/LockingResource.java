package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/locking=LOCKING
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LockingResource extends SimpleResourceDefinition {

    static final PathElement LOCKING_PATH = PathElement.pathElement(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);

    // attributes
    static final SimpleAttributeDefinition ACQUIRE_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ACQUIRE_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.ACQUIRE_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(15000))
                    .build();

    static final SimpleAttributeDefinition CONCURRENCY_LEVEL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CONCURRENCY_LEVEL, ModelType.INT, true)
                    .setXmlName(Attribute.CONCURRENCY_LEVEL.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1000))
                    .build();

    static final SimpleAttributeDefinition ISOLATION =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ISOLATION, ModelType.STRING, true)
                    .setXmlName(Attribute.ISOLATION.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<IsolationLevel>(IsolationLevel.class, true, false))
                    .setDefaultValue(new ModelNode().set(IsolationLevel.REPEATABLE_READ.name()))
                    .build();

    static final SimpleAttributeDefinition STRIPING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STRIPING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.STRIPING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();

    static final AttributeDefinition[] LOCKING_ATTRIBUTES = { ACQUIRE_TIMEOUT, CONCURRENCY_LEVEL, ISOLATION, STRIPING};

    public LockingResource() {
        super(LOCKING_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.LOCKING),
                CacheConfigOperationHandlers.LOCKING_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(LOCKING_ATTRIBUTES);
        for (AttributeDefinition attr : LOCKING_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
