package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource
 *
 *    /subsystem=infinispan/cache-container=X/cache=Y/store=Z/write-behind=WRITE_BEHIND
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreWriteBehindResource extends SimpleResourceDefinition {

    private static final PathElement STORE_WRITE_BEHIND_PATH = PathElement.pathElement(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);
    public static final StoreWriteBehindResource INSTANCE = new StoreWriteBehindResource();

    // attributes
    static final SimpleAttributeDefinition FLUSH_LOCK_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.FLUSH_LOCK_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.FLUSH_LOCK_TIMEOUT.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1))
                    .build();

    static final SimpleAttributeDefinition MODIFICATION_QUEUE_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODIFICATION_QUEUE_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.MODIFICATION_QUEUE_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1024))
                    .build();

    static final SimpleAttributeDefinition SHUTDOWN_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SHUTDOWN_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.SHUTDOWN_TIMEOUT.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(25000))
                    .build();

    static final SimpleAttributeDefinition THREAD_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.THREAD_POOL_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.THREAD_POOL_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1))
                    .build();

    static final AttributeDefinition[] WRITE_BEHIND_ATTRIBUTES = {FLUSH_LOCK_TIMEOUT, MODIFICATION_QUEUE_SIZE, THREAD_POOL_SIZE, SHUTDOWN_TIMEOUT};

    static final ObjectTypeAttributeDefinition WRITE_BEHIND_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.WRITE_BEHIND, WRITE_BEHIND_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("write-behind").
            build();


    public StoreWriteBehindResource() {
        super(STORE_WRITE_BEHIND_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.WRITE_BEHIND),
                CacheConfigOperationHandlers.STORE_WRITE_BEHIND_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(WRITE_BEHIND_ATTRIBUTES);
        for (AttributeDefinition attr : WRITE_BEHIND_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
