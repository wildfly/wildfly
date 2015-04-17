package org.jboss.as.telemetry.extension;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
//import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
//import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetrySubsystemDefinition extends SimpleResourceDefinition {

    public static final TelemetrySubsystemDefinition INSTANCE = new TelemetrySubsystemDefinition();

    protected static final SimpleAttributeDefinition FREQUENCY =
            new SimpleAttributeDefinitionBuilder(TelemetryExtension.FREQUENCY, ModelType.LONG)
                    .setAllowExpression(true)
                    .setXmlName(TelemetryExtension.FREQUENCY)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(1000))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(TelemetryExtension.ENABLED, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setXmlName(TelemetryExtension.ENABLED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowNull(true)
                    .build();

    private TelemetrySubsystemDefinition() {
        super(TelemetryExtension.SUBSYSTEM_PATH,
                TelemetryExtension.getResourceDescriptionResolver(null),
                //We always need to add an 'add' operation
                SubsystemAdd.INSTANCE,
                //Every resource that is added, normally needs a remove operation
                SubsystemRemove.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(FREQUENCY, null, TelemetryFrequencyHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(ENABLED, null, TelemetryEnabledHandler.INSTANCE);
    }
}
