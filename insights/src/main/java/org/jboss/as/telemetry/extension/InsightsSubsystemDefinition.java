package org.jboss.as.insights.extension;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsSubsystemDefinition extends SimpleResourceDefinition {

    public static final InsightsSubsystemDefinition INSTANCE = new InsightsSubsystemDefinition();

    protected static final SimpleAttributeDefinition FREQUENCY =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.FREQUENCY, ModelType.LONG)
                    .setAllowExpression(true)
                    .setXmlName(InsightsExtension.FREQUENCY)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(1000))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.ENABLED, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setXmlName(InsightsExtension.ENABLED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition RHNUID =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.RHNUID, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition RHNPW =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.RHNPW, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXYUSER =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.PROXY_USER, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXYPASSWORD =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.PROXY_PASSWORD, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXYPORT =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.PROXY_PORT, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXYURL =
            new SimpleAttributeDefinitionBuilder(InsightsExtension.PROXY_URL, ModelType.STRING)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(""))
                    .setAllowNull(true)
                    .build();

    private InsightsSubsystemDefinition() {
        super(InsightsExtension.SUBSYSTEM_PATH,
                InsightsExtension.getResourceDescriptionResolver(null),
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
        resourceRegistration.registerReadWriteAttribute(FREQUENCY, null, InsightsFrequencyHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(ENABLED, null, InsightsEnabledHandler.INSTANCE);
    }
}
