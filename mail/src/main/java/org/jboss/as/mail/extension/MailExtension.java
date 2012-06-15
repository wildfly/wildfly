package org.jboss.as.mail.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
    static PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    static PathElement MAIL_SESSION_PATH = PathElement.pathElement(MailSubsystemModel.MAIL_SESSION);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MailExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), parser);
    }

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;



    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

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
