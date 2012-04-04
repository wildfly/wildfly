package org.jboss.as.controller.transform;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
public class SessionDefinition extends SimpleResourceDefinition {
    public static SessionDefinition INSTANCE = new SessionDefinition();

    private SessionDefinition() {
        super(PathElement.pathElement("session"), new TestResourceDescriptionResolver());
    }

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder("jndi-name", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName("jndi-name")
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder("from", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(null)
                    .setXmlName("from")
                    .setRestartAllServices()
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder("debug", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setXmlName("debug")
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();
    private static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, new GlobalOperationHandlers.WriteAttributeHandler());
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        final String name = "dump-session-info";
        DefaultOperationDescriptionProvider desc = new DefaultOperationDescriptionProvider(name, getResourceDescriptionResolver(), DEBUG);
        registry.registerOperationHandler(name, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.completeStep();
            }
        }, desc);

    }
}
