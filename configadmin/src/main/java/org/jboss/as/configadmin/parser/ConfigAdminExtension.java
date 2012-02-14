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
package org.jboss.as.configadmin.parser;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * Domain extension used to initialize the ConfigAdmin subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class ConfigAdminExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "configadmin";

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_0.getUriString(), ConfigAdminParser.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(ConfigAdminProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, ConfigAdminAdd.INSTANCE, ConfigAdminAdd.DESCRIPTION, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, ConfigAdminDescribeHandler.INSTANCE, ConfigAdminAdd.DESCRIPTION, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, ConfigAdminProviders.SUBSYSTEM_REMOVE, false);

        // Configuration Admin Settings
        ManagementResourceRegistration configuration = registration.registerSubModel(PathElement.pathElement(ModelConstants.CONFIGURATION), ConfigAdminProviders.CONFIGURATION_DESCRIPTION);
        configuration.registerOperationHandler(ModelDescriptionConstants.ADD, ConfigurationAdd.INSTANCE, ConfigurationAdd.DESCRIPTION, false);
        configuration.registerOperationHandler(ModelDescriptionConstants.REMOVE, ConfigurationRemove.INSTANCE, ConfigurationRemove.DESCRIPTION, false);

        subsystem.registerXMLElementWriter(ConfigAdminWriter.INSTANCE);
    }
}
