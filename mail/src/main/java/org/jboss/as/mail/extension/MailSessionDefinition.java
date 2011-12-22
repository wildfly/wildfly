package org.jboss.as.mail.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
public class MailSessionDefinition extends SimpleResourceDefinition {
    public static MailSessionDefinition INSTANCE = new MailSessionDefinition();

    private MailSessionDefinition() {
        super(PathElement.pathElement(MailSubsystemModel.MAIL_SESSION),
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION),
                MailSessionAdd.INSTANCE, MailSessionRemove.INSTANCE);
    }

    private static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.JNDI_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    private static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    private static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        rootResourceRegistration.registerReadOnlyAttribute(JNDI_NAME, null);
        rootResourceRegistration.registerReadOnlyAttribute(FROM, null);
        rootResourceRegistration.registerReadOnlyAttribute(DEBUG, null);
    }

}
