package org.jboss.as.mail.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
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
        super(path, MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION+"."+MailSubsystemModel.SERVER_TYPE), MailServerAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    protected static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition SSL =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.SSL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setXmlName(MailSubsystemModel.SSL)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition USERNAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.USER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(MailSubsystemModel.LOGIN_USERNAME)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(MailSubsystemModel.PASSWORD)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        MailServerWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
    }
}
