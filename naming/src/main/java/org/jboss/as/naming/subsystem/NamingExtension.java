/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.naming.management.JndiViewOperation;

/**
 * Domain extension used to initialize the naming subsystem element handlers.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
public class NamingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "naming";
    private static final String NAMESPACE_1_0 = "urn:jboss:domain:naming:1.0";
    private static final String NAMESPACE_1_1 = "urn:jboss:domain:naming:1.1";
    private static final String NAMESPACE_1_2 = "urn:jboss:domain:naming:1.2";
    private static final String NAMESPACE_1_3 = "urn:jboss:domain:naming:1.3";
    private static final String NAMESPACE_1_4 = "urn:jboss:domain:naming:1.4";
    static final String NAMESPACE_2_0 = "urn:jboss:domain:naming:2.0";
    //2.1 introduced in WildFly 10.1
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 1, 0);

    static final String RESOURCE_NAME = NamingExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);

    static final SensitiveTargetAccessConstraintDefinition JNDI_VIEW_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "jndi-view", false, true, true));

    static final SensitiveTargetAccessConstraintDefinition NAMING_BINDING_SENSITIVITY_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "naming-binding", false, false, false));

    static final ApplicationTypeAccessConstraintDefinition NAMING_BINDING_APPLICATION_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(
            new ApplicationTypeConfig(NamingExtension.SUBSYSTEM_NAME, NamingSubsystemModel.BINDING));


    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, NamingExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new NamingSubsystemRootResourceDefinition());

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        registration.registerSubModel(new NamingBindingResourceDefinition());
        registration.registerSubModel(new RemoteNamingResourceDefinition());

        if (context.isRuntimeOnlyRegistrationValid()) {
            registration.registerOperationHandler(NamingSubsystemRootResourceDefinition.JNDI_VIEW, JndiViewOperation.INSTANCE, false);
        }

        subsystem.registerXMLElementWriter(NamingSubsystemXMLPersister.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_0, () -> new NamingSubsystem10Parser(context.getProcessType() == ProcessType.APPLICATION_CLIENT));
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_1, NamingSubsystem11Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_2, NamingSubsystem12Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_3, NamingSubsystem13Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_4, NamingSubsystem14Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_2_0, NamingSubsystem20Parser::new);
    }


}
