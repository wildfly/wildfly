/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
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

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(5, 0, 0);

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

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystem.registerXMLElementWriter(EJB3SubsystemXMLPersister.INSTANCE);

        PathManager pathManager = context.getProcessType().isServer() ? context.getPathManager() : null;
        subsystem.registerSubsystemModel(new EJB3SubsystemRootResourceDefinition(registerRuntimeOnly, pathManager));

        if (registerRuntimeOnly) {
            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME),
                    getResourceDescriptionResolver("deployed")).setFeature(false));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            deploymentsRegistration.registerSubModel(MessageDrivenBeanResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(SingletonBeanDeploymentResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(StatelessSessionBeanDeploymentResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(StatefulSessionBeanDeploymentResourceDefinition.INSTANCE);
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
    }
}
