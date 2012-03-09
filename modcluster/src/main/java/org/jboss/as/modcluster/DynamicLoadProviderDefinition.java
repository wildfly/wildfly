package org.jboss.as.modcluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class DynamicLoadProviderDefinition extends SimpleResourceDefinition {
    protected static final DynamicLoadProviderDefinition INSTANCE = new DynamicLoadProviderDefinition();

    static final SimpleAttributeDefinition DECAY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.DECAY, ModelType.INT, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR))
            .build();

    static final SimpleAttributeDefinition HISTORY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.HISTORY, ModelType.INT, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY))
            .build();

    private DynamicLoadProviderDefinition() {
        super(ModClusterExtension.DYNAMIC_LOAD_PROVIDER,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER),
                DynamicLoadProviderAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
    }

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            HISTORY, DECAY
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }
}
