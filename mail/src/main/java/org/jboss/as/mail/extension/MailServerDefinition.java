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
 * @created 19.12.11 21:05
 */
public class MailServerDefinition extends SimpleResourceDefinition {
    public static final MailServerDefinition INSTANCE_SMTP = new MailServerDefinition(MailSubsystemModel.SMTP_SERVER_PATH);
    public static final MailServerDefinition INSTANCE_IMAP = new MailServerDefinition(MailSubsystemModel.IMAP_SERVER_PATH);
    public static final MailServerDefinition INSTANCE_POP3 = new MailServerDefinition(MailSubsystemModel.POP3_SERVER_PATH);

    private MailServerDefinition(PathElement path) {
        super(path, MailExtension.getResourceDescriptionResolver(MailSubsystemModel.SERVER_TYPE), MailServerAdd.INSTANCE, MailServerRemove.INSTANCE);
    }

    private static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    private static final SimpleAttributeDefinition SSL =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.SSL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    private static final SimpleAttributeDefinition USERNAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.USER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    private static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(OUTBOUND_SOCKET_BINDING_REF, null);
        resourceRegistration.registerReadOnlyAttribute(SSL, null);
        resourceRegistration.registerReadOnlyAttribute(USERNAME, null);
        resourceRegistration.registerReadOnlyAttribute(PASSWORD, null);
    }
}
