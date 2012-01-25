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

package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerLogger.ROOT_LOGGER;

/**
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerExtension implements Extension {

    public static final String SUBSYSTEM_NAME = CommonAttributes.DEPLOYMENT_SCANNER;
    protected static final PathElement SCANNERS_PATH = PathElement.pathElement("scanner");
    static final String DEFAULT_SCANNER_NAME = "default"; // we actually need a scanner name to make it addressable
    private static final String RESOURCE_NAME = DeploymentScannerExtension.class.getPackage().getName() + ".LocalDescriptions";


    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, DeploymentScannerExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debug("Initializing Deployment Scanner Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(CommonAttributes.DEPLOYMENT_SCANNER, 1, 0);
        subsystem.registerXMLElementWriter(DeploymentScannerParser_1_1.INSTANCE);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new DeploymentScannerSubsystemDefinition());
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerSubModel(DeploymentScannerDefinition.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DEPLOYMENT_SCANNER_1_0.getUriString(), DeploymentScannerParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DEPLOYMENT_SCANNER_1_1.getUriString(), DeploymentScannerParser_1_1.INSTANCE);

    }

}
