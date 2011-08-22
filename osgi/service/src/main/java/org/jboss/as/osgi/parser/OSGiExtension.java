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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.MODULE;
import static org.jboss.as.osgi.parser.CommonAttributes.PROPERTY;

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
        registration.registerOperationHandler(ADD, OSGiSubsystemAdd.INSTANCE, OSGiSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, OSGiSubsystemDescribeHandler.INSTANCE, OSGiSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerReadWriteAttribute(CommonAttributes.ACTIVATION, null, new ActivationWriteHandler(), Storage.CONFIGURATION);

        // Configuration Admin Setings
        ManagementResourceRegistration casConfigs = registration.registerSubModel(PathElement.pathElement(CONFIGURATION), OSGiSubsystemProviders.OSGI_CONFIGURATION_RESOURCE);
        casConfigs.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiCasConfigAdd.INSTANCE, OSGiCasConfigAdd.INSTANCE, false);
        casConfigs.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiCasConfigRemove.INSTANCE, OSGiCasConfigRemove.INSTANCE, false);

        // Framework Properties
        ManagementResourceRegistration properties = registration.registerSubModel(PathElement.pathElement(PROPERTY), OSGiSubsystemProviders.OSGI_PROPERTY_RESOURCE);
        properties.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiPropertyAdd.INSTANCE, OSGiPropertyAdd.INSTANCE, false);
        properties.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiPropertyRemove.INSTANCE, OSGiPropertyRemove.INSTANCE, false);

        // Pre loaded modules
        ManagementResourceRegistration modules = registration.registerSubModel(PathElement.pathElement(MODULE), OSGiSubsystemProviders.OSGI_MODULE_RESOURCE);
        modules.registerOperationHandler(ModelDescriptionConstants.ADD, OSGiModuleAdd.INSTANCE, OSGiModuleAdd.INSTANCE, false);
        modules.registerOperationHandler(ModelDescriptionConstants.REMOVE, OSGiModuleRemove.INSTANCE, OSGiModuleRemove.INSTANCE, false);

        subsystem.registerXMLElementWriter(parser);
    }
}
