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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerLogger.ROOT_LOGGER;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "deployment-scanner";
    private static final PathElement scannersPath = PathElement.pathElement("scanner");
    private static final DeploymentScannerParser parser = new DeploymentScannerParser();
    private static final String DEFAULT_SCANNER_NAME = "default"; // we actually need a scanner name to make it addressable

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
        subsystem.registerXMLElementWriter(parser);
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
        scanners.registerReadWriteAttribute(Attribute.DEPLOYMENT_TIMEOUT.getLocalName(), null, WriteDeploymentTimeoutAttributeHandler.INSTANCE, Storage.CONFIGURATION);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), parser);
    }

    static class DeploymentScannerParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode scanners = context.getModelNode();
            for (final Property list : scanners.asPropertyList()) {
                final ModelNode node = list.getValue();

                for (final Property scanner : node.asPropertyList()) {

                    writer.writeEmptyElement(Element.DEPLOYMENT_SCANNER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), scanner.getName());
                    ModelNode configuration = scanner.getValue();
                    if (configuration.hasDefined(CommonAttributes.PATH)) {
                        writer.writeAttribute(Attribute.PATH.getLocalName(), configuration.get(CommonAttributes.PATH)
                                .asString());
                    }
                    if (configuration.hasDefined(CommonAttributes.SCAN_ENABLED)) {
                        writer.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(),
                                configuration.get(CommonAttributes.SCAN_ENABLED).asString());
                    }
                    if (configuration.hasDefined(CommonAttributes.SCAN_INTERVAL)) {
                        writer.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(),
                                configuration.get(CommonAttributes.SCAN_INTERVAL).asString());
                    }
                    if (configuration.hasDefined(CommonAttributes.RELATIVE_TO)) {
                        writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(),
                                configuration.get(CommonAttributes.RELATIVE_TO).asString());
                    }
                    if (configuration.hasDefined(CommonAttributes.AUTO_DEPLOY_ZIPPED)) {
                        if (!configuration.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).asBoolean()) {
                            writer.writeAttribute(Attribute.AUTO_DEPLOY_ZIPPED.getLocalName(), Boolean.FALSE.toString());
                        }
                    }
                    if (configuration.hasDefined(CommonAttributes.AUTO_DEPLOY_EXPLODED)) {
                        if (configuration.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).asBoolean()) {
                            writer.writeAttribute(Attribute.AUTO_DEPLOY_EXPLODED.getLocalName(), Boolean.TRUE.toString());
                        }
                    }
                    if (configuration.hasDefined(CommonAttributes.DEPLOYMENT_TIMEOUT)) {
                        writer.writeAttribute(Attribute.DEPLOYMENT_TIMEOUT.getLocalName(), configuration.get(CommonAttributes.DEPLOYMENT_TIMEOUT).asString());
                    }
                }
                writer.writeEndElement();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            requireNoAttributes(reader);

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DEPLOYMENT_SCANNER_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case DEPLOYMENT_SCANNER: {
                                //noinspection unchecked
                                parseScanner(reader, address, list);
                                break;
                            }
                            default: throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default: throw unexpectedElement(reader);
                }
            }
        }

        void parseScanner(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
            // Handle attributes
            Boolean enabled = null;
            Integer interval = null;
            String path = null;
            String name = DEFAULT_SCANNER_NAME;
            String relativeTo = null;
            Boolean autoDeployZipped = null;
            Boolean autoDeployExploded = null;
            Long deploymentTimeout = null;
            final int attrCount = reader.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH: {
                        path = value;
                        break;
                    }
                    case NAME: {
                        name = value;
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = value;
                        break;
                    }
                    case SCAN_INTERVAL: {
                        interval = Integer.parseInt(value);
                        break;
                    }
                    case SCAN_ENABLED: {
                        enabled = Boolean.parseBoolean(value);
                        break;
                    }
                    case AUTO_DEPLOY_ZIPPED: {
                        autoDeployZipped = Boolean.parseBoolean(value);
                        break;
                    }
                    case AUTO_DEPLOY_EXPLODED: {
                        autoDeployExploded = Boolean.parseBoolean(value);
                        break;
                    }
                    case DEPLOYMENT_TIMEOUT: {
                        deploymentTimeout = Long.parseLong(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            if (name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(CommonAttributes.NAME));
            }
            if (path == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(CommonAttributes.PATH));
            }
            requireNoContent(reader);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address).add(CommonAttributes.SCANNER, name);
            operation.get(CommonAttributes.PATH).set(path);
            if (interval != null) operation.get(CommonAttributes.SCAN_INTERVAL).set(interval.intValue());
            if (autoDeployZipped != null) operation.get(CommonAttributes.AUTO_DEPLOY_ZIPPED).set(autoDeployZipped.booleanValue());
            if (autoDeployExploded != null) operation.get(CommonAttributes.AUTO_DEPLOY_EXPLODED).set(autoDeployExploded.booleanValue());
            if (enabled != null) operation.get(CommonAttributes.SCAN_ENABLED).set(enabled.booleanValue());
            if(relativeTo != null) operation.get(CommonAttributes.RELATIVE_TO).set(relativeTo);
            if(deploymentTimeout != null) operation.get(CommonAttributes.DEPLOYMENT_TIMEOUT).set(deploymentTimeout);
            list.add(operation);
        }

    }

}
