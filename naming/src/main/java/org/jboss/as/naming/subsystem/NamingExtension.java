/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
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
    static final String NAMESPACE_1_3 = "urn:jboss:domain:naming:1.3";

    static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    static final int MANAGEMENT_API_MINOR_VERSION = 2;
    static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static final String RESOURCE_NAME = NamingExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, NamingExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(NamingSubsystemRootResourceDefinition.INSTANCE);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        registration.registerSubModel(NamingBindingResourceDefinition.INSTANCE);
        registration.registerSubModel(RemoteNamingResourceDefinition.INSTANCE);

        if (context.isRuntimeOnlyRegistrationValid()) {
            registration.registerOperationHandler(NamingSubsystemRootResourceDefinition.JNDI_VIEW, JndiViewOperation.INSTANCE, false);
        }

        subsystem.registerXMLElementWriter(NamingSubsystem13Parser.INSTANCE);

        if (context.isRegisterTransformers()) {
            // register 1.1.0 transformer
            final TransformersSubRegistration transformersSubRegistration110 = subsystem.registerModelTransformers(ModelVersion.create(1, 1, 0), ResourceTransformer.DEFAULT);
            transformersSubRegistration110.registerSubResource(NamingSubsystemModel.BINDING_PATH,new Naming11Transformer());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_0, NamingSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_1, NamingSubsystem11Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_2, NamingSubsystem12Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_3, NamingSubsystem13Parser.INSTANCE);
    }


}
