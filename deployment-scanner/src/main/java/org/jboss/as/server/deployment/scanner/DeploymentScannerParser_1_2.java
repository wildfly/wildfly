/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 */
class DeploymentScannerParser_1_2 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final DeploymentScannerParser_1_2 INSTANCE = new DeploymentScannerParser_1_2();

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode scanners = context.getModelNode();
        for (final Property list : scanners.asPropertyList()) {

            final ModelNode node = list.getValue();

            for (final Property scanner : node.asPropertyList()) {

                final String scannerName = scanner.getName();
                final ModelNode configuration = scanner.getValue();

                writer.writeEmptyElement(Element.DEPLOYMENT_SCANNER.getLocalName());

                if (!DeploymentScannerExtension.DEFAULT_SCANNER_NAME.equals(scannerName)) {
                    writer.writeAttribute(Attribute.NAME.getLocalName(), scannerName);
                }

                DeploymentScannerDefinition.PATH.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.RELATIVE_TO.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.SCAN_ENABLED.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.SCAN_INTERVAL.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.AUTO_DEPLOY_ZIPPED.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.AUTO_DEPLOY_XML.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.DEPLOYMENT_TIMEOUT.marshallAsAttribute(configuration, writer);
                DeploymentScannerDefinition.RUNTIME_FAILURE_CAUSES_ROLLBACK.marshallAsAttribute(configuration, writer);
            }
            writer.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, DeploymentScannerExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DEPLOYMENT_SCANNER_1_0:
                case DEPLOYMENT_SCANNER_1_1:
                case DEPLOYMENT_SCANNER_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT_SCANNER: {
                            //noinspection unchecked
                            parseScanner(reader, address, list);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    void parseScanner(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
        // Handle attributes

        String name = DeploymentScannerExtension.DEFAULT_SCANNER_NAME;
        String path = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int attrCount = reader.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH: {
                    path = value;
                    DeploymentScannerDefinition.PATH.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case NAME: {
                    name = DeploymentScannerDefinition.NAME.parse(value,reader).asString();
                    break;
                }
                case RELATIVE_TO: {
                    DeploymentScannerDefinition.RELATIVE_TO.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case SCAN_INTERVAL: {
                    DeploymentScannerDefinition.SCAN_INTERVAL.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case SCAN_ENABLED: {
                    DeploymentScannerDefinition.SCAN_ENABLED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_ZIPPED: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_ZIPPED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_EXPLODED: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case AUTO_DEPLOY_XML: {
                    DeploymentScannerDefinition.AUTO_DEPLOY_XML.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case DEPLOYMENT_TIMEOUT: {
                    DeploymentScannerDefinition.DEPLOYMENT_TIMEOUT.parseAndSetParameter(value,operation,reader);
                    break;
                }
                case RUNTIME_FAILURE_CAUSES_ROLLBACK: {
                    DeploymentScannerDefinition.RUNTIME_FAILURE_CAUSES_ROLLBACK.parseAndSetParameter(value,operation,reader);
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
        operation.get(OP_ADDR).set(address).add(CommonAttributes.SCANNER, name);
        list.add(operation);
    }

}
