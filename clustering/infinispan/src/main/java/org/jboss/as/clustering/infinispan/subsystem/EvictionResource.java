package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/eviction=EVICTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class EvictionResource extends SimpleResourceDefinition {

    private static final PathElement EVICTION_PATH = PathElement.pathElement(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
    public static final EvictionResource INSTANCE = new EvictionResource();

    // attributes
    static final SimpleAttributeDefinition EVICTION_STRATEGY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STRATEGY, ModelType.STRING, true)
                    .setXmlName(Attribute.STRATEGY.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<EvictionStrategy>(EvictionStrategy.class, true, false))
                    .setDefaultValue(new ModelNode().set(EvictionStrategy.NONE.name()))
                    .build();

    static final SimpleAttributeDefinition MAX_ENTRIES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MAX_ENTRIES, ModelType.INT, true)
                    .setXmlName(Attribute.MAX_ENTRIES.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(-1))
                    .build();

    static final AttributeDefinition[] EVICTION_ATTRIBUTES = {EVICTION_STRATEGY, MAX_ENTRIES};

    public EvictionResource() {
        super(EVICTION_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.EVICTION),
                CacheConfigOperationHandlers.EVICTION_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(EVICTION_ATTRIBUTES);
        for (AttributeDefinition attr : EVICTION_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
