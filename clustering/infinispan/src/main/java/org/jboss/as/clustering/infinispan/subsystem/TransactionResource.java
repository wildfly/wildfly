package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.transaction.LockingMode;
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
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/transaction=TRANSACTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransactionResource extends SimpleResourceDefinition {

    private static final PathElement TRANSACTION_PATH = PathElement.pathElement(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);

    // attributes
    // cache mode required, txn mode not
    static final SimpleAttributeDefinition LOCKING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LOCKING, ModelType.STRING, true)
                    .setXmlName(Attribute.LOCKING.getLocalName())
                    .setAllowExpression(false)
                    .setValidator(new EnumValidator<LockingMode>(LockingMode.class, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(LockingMode.OPTIMISTIC.name()))
                    .build();
    static final SimpleAttributeDefinition MODE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, true)
                    .setXmlName(Attribute.MODE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<TransactionMode>(TransactionMode.class, true, true))
                    .setDefaultValue(new ModelNode().set(TransactionMode.NONE.name()))
                    .build();
    static final SimpleAttributeDefinition STOP_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STOP_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.STOP_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(30000))
                    .build();

    static final AttributeDefinition[] TRANSACTION_ATTRIBUTES = {MODE, STOP_TIMEOUT, LOCKING};

    public TransactionResource() {
        super(TRANSACTION_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.TRANSACTION),
                CacheConfigOperationHandlers.TRANSACTION_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(TRANSACTION_ATTRIBUTES);
        for (AttributeDefinition attr : TRANSACTION_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
