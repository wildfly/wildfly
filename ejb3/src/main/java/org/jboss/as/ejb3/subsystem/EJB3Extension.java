/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition.Parameters;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatelessSessionBeanDeploymentResourceDefinition;

/**
 * Extension that provides the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3Extension implements Extension {

    public static final String SUBSYSTEM_NAME = "ejb3";
    public static final String NAMESPACE_1_0 = EJB3SubsystemNamespace.EJB3_1_0.getUriString();
    public static final String NAMESPACE_1_1 = EJB3SubsystemNamespace.EJB3_1_1.getUriString();
    public static final String NAMESPACE_1_2 = EJB3SubsystemNamespace.EJB3_1_2.getUriString();
    public static final String NAMESPACE_1_3 = EJB3SubsystemNamespace.EJB3_1_3.getUriString();
    public static final String NAMESPACE_1_4 = EJB3SubsystemNamespace.EJB3_1_4.getUriString();
    public static final String NAMESPACE_1_5 = EJB3SubsystemNamespace.EJB3_1_5.getUriString();
    public static final String NAMESPACE_2_0 = EJB3SubsystemNamespace.EJB3_2_0.getUriString();
    public static final String NAMESPACE_3_0 = EJB3SubsystemNamespace.EJB3_3_0.getUriString();
    public static final String NAMESPACE_4_0 = EJB3SubsystemNamespace.EJB3_4_0.getUriString();
    public static final String NAMESPACE_5_0 = EJB3SubsystemNamespace.EJB3_5_0.getUriString();
    public static final String NAMESPACE_6_0 = EJB3SubsystemNamespace.EJB3_6_0.getUriString();
    public static final String NAMESPACE_7_0 = EJB3SubsystemNamespace.EJB3_7_0.getUriString();
    public static final String NAMESPACE_8_0 = EJB3SubsystemNamespace.EJB3_8_0.getUriString();
    public static final String NAMESPACE_9_0 = EJB3SubsystemNamespace.EJB3_9_0.getUriString();
    public static final String NAMESPACE_10_0 = EJB3SubsystemNamespace.EJB3_10_0.getUriString();

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = EJB3Extension.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EJB3Extension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, EJB3Model.CURRENT.getVersion());

        subsystem.registerXMLElementWriter(EJB3SubsystemXMLPersister.INSTANCE);

        PathManager pathManager = context.getProcessType().isServer() ? context.getPathManager() : null;
        subsystem.registerSubsystemModel(new EJB3SubsystemRootResourceDefinition(registerRuntimeOnly, pathManager));

        if (registerRuntimeOnly) {
            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME),
                    getResourceDescriptionResolver("deployed")).setFeature(false).setRuntime());
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            deploymentsRegistration.registerSubModel(new MessageDrivenBeanResourceDefinition());
            deploymentsRegistration.registerSubModel(new SingletonBeanDeploymentResourceDefinition());
            deploymentsRegistration.registerSubModel(new StatelessSessionBeanDeploymentResourceDefinition());
            deploymentsRegistration.registerSubModel(new StatefulSessionBeanDeploymentResourceDefinition());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_0, EJB3Subsystem10Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_1, EJB3Subsystem11Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_2, EJB3Subsystem12Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_3, EJB3Subsystem13Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_4, EJB3Subsystem14Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_5, EJB3Subsystem15Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_2_0, EJB3Subsystem20Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_3_0, EJB3Subsystem30Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_4_0, EJB3Subsystem40Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_5_0, EJB3Subsystem50Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_6_0, EJB3Subsystem60Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_7_0, EJB3Subsystem70Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_8_0, EJB3Subsystem80Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_9_0, EJB3Subsystem90Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_10_0, EJB3Subsystem100Parser::new);
    }
}
