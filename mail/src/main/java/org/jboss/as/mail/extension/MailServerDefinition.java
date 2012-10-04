package org.jboss.as.mail.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @since 7.1.0
 */
class MailServerDefinition extends SimpleResourceDefinition {
    protected static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING_REF_OPTIONAL = SimpleAttributeDefinitionBuilder.create(OUTBOUND_SOCKET_BINDING_REF)
            .setAllowNull(true)
            .build();

    protected static final SimpleAttributeDefinition SSL =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.SSL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();


    protected static final SimpleAttributeDefinition TLS =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.TLS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
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
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    protected static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.PROPERTIES, true)
            .setXmlName("property")
            .setWrapXmlElement(false)
            .build();


    static final AttributeDefinition[] ATTRIBUTES = {OUTBOUND_SOCKET_BINDING_REF, SSL, TLS, USERNAME, PASSWORD, PROPERTIES};
    static final AttributeDefinition[] ATTRIBUTES_CUSTOM = {OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, SSL, TLS, USERNAME, PASSWORD, PROPERTIES};


    public static final MailServerDefinition INSTANCE_SMTP = new MailServerDefinition(MailSubsystemModel.SMTP_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_IMAP = new MailServerDefinition(MailSubsystemModel.IMAP_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_POP3 = new MailServerDefinition(MailSubsystemModel.POP3_SERVER_PATH, ATTRIBUTES);
    public static final MailServerDefinition INSTANCE_CUSTOM = new MailServerDefinition(MailSubsystemModel.CUSTOM_SERVER_PATH, ATTRIBUTES_CUSTOM);

    private final AttributeDefinition[] attributes;

    private MailServerDefinition(final PathElement path, AttributeDefinition[] attributes) {
        super(path,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION, MailSubsystemModel.SERVER_TYPE),
                new MailServerAdd(attributes),
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.attributes = attributes;
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : this.attributes) {
            resourceRegistration.registerReadWriteAttribute(attr, null, MailServerWriteAttributeHandler.INSTANCE);
        }
    }

}
