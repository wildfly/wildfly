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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * Domain extension used to initialize the OSGi subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
public class OSGiExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "osgi";

    private final OSGiSubsystemParser parser = new OSGiSubsystemParser();

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(OSGiSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiSubsystemAdd.INSTANCE, OSGiSubsystemAdd.DESCRIPTION, false);
        registration.registerReadWriteAttribute(CommonAttributes.ACTIVATION, null, new ActivationAttributeHandler(), Storage.CONFIGURATION);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, OSGiSubsystemDescribeHandler.INSTANCE, OSGiSubsystemAdd.DESCRIPTION, false, OperationEntry.EntryType.PRIVATE);

        // Configuration Admin Setings
        ManagementResourceRegistration configuration = registration.registerSubModel(PathElement.pathElement(CommonAttributes.CONFIGURATION), OSGiSubsystemProviders.CONFIGURATION_DESCRIPTION);
        configuration.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiConfigurationAdd.INSTANCE, OSGiConfigurationAdd.DESCRIPTION, false);
        configuration.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiConfigurationRemove.INSTANCE, OSGiConfigurationRemove.DESCRIPTION, false);

        // Framework Properties
        ManagementResourceRegistration properties = registration.registerSubModel(PathElement.pathElement(CommonAttributes.FRAMEWORK_PROPERTY), OSGiSubsystemProviders.FRAMEWORK_PROPERTY_DESCRIPTION);
        properties.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiFrameworkPropertyAdd.INSTANCE, OSGiFrameworkPropertyAdd.DESCRIPTION, false);
        properties.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiFrameworkPropertyRemove.INSTANCE, OSGiFrameworkPropertyRemove.DESCRIPTION, false);

        // Pre loaded modules
        ManagementResourceRegistration capabilities = registration.registerSubModel(PathElement.pathElement(CommonAttributes.CAPABILITY), OSGiSubsystemProviders.CAPABILITY_DESCRIPTION);
        capabilities.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiCapabilityAdd.INSTANCE, OSGiCapabilityAdd.DESCRIPTION, false);
        capabilities.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiCapabilityRemove.INSTANCE, OSGiCapabilityRemove.DESCRIPTION, false);

        subsystem.registerXMLElementWriter(parser);
    }
}
