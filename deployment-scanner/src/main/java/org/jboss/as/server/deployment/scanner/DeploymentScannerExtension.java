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

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.server.deployment.scanner.DeploymentScannerLogger.ROOT_LOGGER;

/**
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "deployment-scanner";
    private static final PathElement scannersPath = PathElement.pathElement("scanner");
    static final String DEFAULT_SCANNER_NAME = "default"; // we actually need a scanner name to make it addressable

    private static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return DeploymentSubsystemDescriptions.getSubsystemDescription(locale);
        }
    };

    private static final DescriptionProvider SCANNER = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return DeploymentSubsystemDescriptions.getScannerDescription(locale);
        }
    };

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debug("Initializing Deployment Scanner Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(CommonAttributes.DEPLOYMENT_SCANNER, 1, 0);
        subsystem.registerXMLElementWriter(DeploymentScannerParser_1_1.INSTANCE);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SUBSYSTEM);
        registration.registerOperationHandler(DeploymentScannerSubsystemAdd.OPERATION_NAME, DeploymentScannerSubsystemAdd.INSTANCE,
                DeploymentScannerSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DeploymentScannerSubsystemRemove.OPERATION_NAME, DeploymentScannerSubsystemRemove.INSTANCE,
                DeploymentScannerSubsystemRemove.INSTANCE, false);
        // Register operation handlers
        final ManagementResourceRegistration scanners = registration.registerSubModel(scannersPath, SCANNER);
        scanners.registerOperationHandler(DeploymentScannerAdd.OPERATION_NAME, DeploymentScannerAdd.INSTANCE, DeploymentScannerAdd.INSTANCE, false);
        scanners.registerOperationHandler(DeploymentScannerRemove.OPERATION_NAME, DeploymentScannerRemove.INSTANCE, DeploymentScannerRemove.INSTANCE, false);
        scanners.registerReadWriteAttribute(Attribute.PATH.getLocalName(), null, WritePathAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.RELATIVE_TO.getLocalName(), null, WriteRelativeToAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.SCAN_ENABLED.getLocalName(), null, WriteEnabledAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.SCAN_INTERVAL.getLocalName(), null, WriteScanIntervalAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.AUTO_DEPLOY_ZIPPED.getLocalName(), null, WriteAutoDeployZipAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.AUTO_DEPLOY_EXPLODED.getLocalName(), null, WriteAutoDeployExplodedAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.AUTO_DEPLOY_XML.getLocalName(), null, WriteAutoDeployXMLAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        scanners.registerReadWriteAttribute(Attribute.DEPLOYMENT_TIMEOUT.getLocalName(), null, WriteDeploymentTimeoutAttributeHandler.INSTANCE, Storage.CONFIGURATION);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DEPLOYMENT_SCANNER_1_0.getUriString(), DeploymentScannerParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.DEPLOYMENT_SCANNER_1_1.getUriString(), DeploymentScannerParser_1_1.INSTANCE);

    }

}
