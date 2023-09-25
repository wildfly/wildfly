/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Domain extension used to initialize the jaxrs subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 */
public class JaxrsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jaxrs";

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(4, 0, 0);

    private static final String RESOURCE_NAME = JaxrsExtension.class.getPackage().getName() + ".LocalDescriptions";
    static PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JaxrsExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final ExtensionContext context) {
        JAXRS_LOGGER.debug("Activating Jakarta RESTful Web Services Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new JaxrsSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        ManagementResourceRegistration jaxrsResReg = subsystem.registerDeploymentModel(new JaxrsDeploymentDefinition());
        jaxrsResReg.registerSubModel(new DeploymentRestResourcesDefintion());
        subsystem.registerXMLElementWriter(JaxrsSubsystemParser_3_0::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, "urn:jboss:domain:jaxrs:1.0", JaxrsSubsystemParser_1_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, "urn:jboss:domain:jaxrs:2.0", JaxrsSubsystemParser_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, "urn:jboss:domain:jaxrs:3.0", JaxrsSubsystemParser_3_0::new);
    }
}
