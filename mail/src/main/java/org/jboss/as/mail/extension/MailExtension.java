/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM_SERVER_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP_SERVER_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.TLS;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;


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

        if (context.isRegisterTransformers()) {
            registerTransformers(subsystem);
        }
        subsystem.registerXMLElementWriter(parser);
    }

    private void registerTransformers(SubsystemRegistration subsystem) {
        TransformersSubRegistration sessionTransformers = subsystem.registerModelTransformers(ModelVersion.create(1, 1, 0), null).registerSubResource(MAIL_SESSION_PATH);
        TransformationDescription smtpServerTransformers = TransformationDescriptionBuilder.Factory.createInstance(PathElement.pathElement(SERVER_TYPE))
                .getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, TLS)
                .end()
                .build();
        TransformationDescription.Tools.register(smtpServerTransformers,sessionTransformers);
        sessionTransformers.registerSubResource(CUSTOM_SERVER_PATH, true);
        //todo i think this should work with builders:
        /*TransformationDescription transformers = TransformationDescriptionBuilder.Factory.createInstance(MAIL_SESSION_PATH)
                .addChildResource(PathElement.pathElement(SERVER_TYPE))
                .getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, TLS)
                .end()
                .end() / parent() //this part is not here now
                .discardChildResource(CUSTOM_SERVER_PATH)
                .build();
        TransformationDescription.Tools.register(transformers,subsystemTransformers);

        also why is DiscardUndefinedAttributesTransformer different from .setDiscard(DiscardAttributeChecker.UNDEFINED, TLS) ?
                */

    }

}
