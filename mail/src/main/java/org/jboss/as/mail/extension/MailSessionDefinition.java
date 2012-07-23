package org.jboss.as.mail.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
public class MailSessionDefinition extends SimpleResourceDefinition {
    public static MailSessionDefinition INSTANCE = new MailSessionDefinition();

    private MailSessionDefinition() {
        super(MailExtension.MAIL_SESSION_PATH,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION),
                MailSessionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.JNDI_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }


}
