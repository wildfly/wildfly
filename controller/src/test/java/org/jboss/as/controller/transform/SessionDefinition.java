package org.jboss.as.controller.transform;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.NoopOperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.test.TestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
public class SessionDefinition extends SimpleResourceDefinition {
    public static SessionDefinition INSTANCE = new SessionDefinition();

    private SessionDefinition() {
        super(PathElement.pathElement("session"), new NonResolvingResourceDescriptionResolver());
    }

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder("jndi-name", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder("from", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(null)
                    .setRestartAllServices()
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder("debug", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();
    private static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, new WriteAttributeHandlers.WriteAttributeOperationHandler(){});
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);
        registry.registerOperationHandler(TestUtils.createOperationDefinition("dump-session-info",DEBUG), NoopOperationStepHandler.WITHOUT_RESULT);
    }
}
