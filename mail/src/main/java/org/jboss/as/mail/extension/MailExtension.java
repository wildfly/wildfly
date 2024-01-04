/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

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


/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @since Jboss AS 7.1.0
 */
public class MailExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "mail";
    private static final String RESOURCE_NAME = MailExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement MAIL_SESSION_PATH = PathElement.pathElement(MailSubsystemModel.MAIL_SESSION);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MailExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_1_0.getUriString(), MailSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_1_1.getUriString(), MailSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_1_2.getUriString(), MailSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_2_0.getUriString(), MailSubsystemParser2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_3_0.getUriString(), MailSubsystemParser3_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.MAIL_4_0.getUriString(), MailSubsystemParser4_0::new);
    }

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(4, 0, 0);


    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(new MailSubsystemDefinition());
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        subsystem.registerXMLElementWriter(new MailSubsystemParser4_0());
    }

}
