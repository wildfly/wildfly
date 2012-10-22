package org.jboss.as.mail.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM_SERVER_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP_SERVER_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.TLS;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;


/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @since 7.1.0
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
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_1_0.getUriString(), parser);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_1_1.getUriString(), parser);
    }

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(MailSubsystemResource.INSTANCE);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        // /subsystem=mail/mail-session
        ManagementResourceRegistration session = subsystemRegistration.registerSubModel(MailSessionDefinition.INSTANCE);
        // /subsystem=mail/mail-session=java:/Mail/server=imap
        session.registerSubModel(MailServerDefinition.INSTANCE_IMAP);
        // /subsystem=mail/mail-session=java:/Mail/server=pop3
        session.registerSubModel(MailServerDefinition.INSTANCE_POP3);
        // /subsystem=mail/mail-session=java:/Mail/server=smtp
        session.registerSubModel(MailServerDefinition.INSTANCE_SMTP);
        // /subsystem=mail/mail-session=java:/Mail/custom=*
        session.registerSubModel(MailServerDefinition.INSTANCE_CUSTOM);


        subsystem.registerXMLElementWriter(parser);
        TransformersSubRegistration sessionTransformers = subsystem.registerModelTransformers(ModelVersion.create(1, 1, 0), null)
                .registerSubResource(MAIL_SESSION_PATH);
        TransformersSubRegistration serverTransformers = sessionTransformers
                .registerSubResource(SMTP_SERVER_PATH, new ResourceTransformer() {
                    @Override
                    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                        ModelNode serverModel = resource.getModel();
                        if (serverModel.has(TLS)) {
                            serverModel.remove(TLS);
                        }
                    }
                });
        serverTransformers.registerOperationTransformer(ADD, new AbstractOperationTransformer() {
            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                if (operation.has(TLS)) {
                    operation.remove(TLS);
                }
                return operation;
            }
        });
        sessionTransformers.registerSubResource(CUSTOM_SERVER_PATH, true);
    }

}
