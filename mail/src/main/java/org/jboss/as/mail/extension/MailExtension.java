package org.jboss.as.mail.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;


/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
public class MailExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "mail";
    private final MailSubsystemParser parser = new MailSubsystemParser();
    private static final String RESOURCE_NAME = MailExtension.class.getPackage().getName() + ".LocalDescriptions";

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, MailExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(MailSubsystemResource.INSTANCE);
        subsystemRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        // /subsystem=mail/mail-session
        ManagementResourceRegistration session = subsystemRegistration.registerSubModel(MailSessionDefinition.INSTANCE);
        // /subsystem=mail/mail-session=java:/Mail/server=imap
        session.registerSubModel(MailServerDefinition.INSTANCE_IMAP);
        // /subsystem=mail/mail-session=java:/Mail/server=pop3
        session.registerSubModel(MailServerDefinition.INSTANCE_POP3);
        // /subsystem=mail/mail-session=java:/Mail/server=smtp
        session.registerSubModel(MailServerDefinition.INSTANCE_SMTP);
        subsystem.registerXMLElementWriter(parser);
    }

}
