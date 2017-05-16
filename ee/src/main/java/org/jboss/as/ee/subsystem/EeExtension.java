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

package org.jboss.as.ee.subsystem;

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
import org.jboss.as.ee.component.deployers.DefaultBindingsConfigurationProcessor;

/**
 * JBossAS domain extension used to initialize the ee subsystem handlers and associated classes.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "ee";
    private static final String RESOURCE_NAME = EeExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(4, 0, 0);

    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EeExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(EeSubsystemRootResource.create());

        // Mandatory describe operation
        rootResource.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // register submodels
        rootResource.registerSubModel(ContextServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedThreadFactoryResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedExecutorServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedScheduledExecutorServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(new DefaultBindingsResourceDefinition(new DefaultBindingsConfigurationProcessor()));

        subsystem.registerXMLElementWriter(EESubsystemXmlPersister.INSTANCE);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_0.getUriString(), EESubsystemParser10::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_1.getUriString(), EESubsystemParser11::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_2.getUriString(), EESubsystemParser12::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_2_0.getUriString(), EESubsystemParser20::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_3_0.getUriString(), EESubsystemParser20::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_4_0.getUriString(), EESubsystemParser40::new);
        context.setProfileParsingCompletionHandler(new BeanValidationProfileParsingCompletionHandler());
    }


}
