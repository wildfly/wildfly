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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.model.ParseUtils.requireNoAttributes;
import static org.jboss.as.model.ParseUtils.requireNoContent;
import static org.jboss.as.model.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class NewDeploymentScannerExtension implements NewExtension {

    private static final DeploymentScannerParser parser = new DeploymentScannerParser();

    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(CommonAttributes.DEPLOYMENT_SCANNER);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(NewDeploymentSubsystemProviders.SUBSYSTEM);
        // Register operation handlers
        registration.registerOperationHandler("add-scanner", NewDeploymentScannerAdd.INSTANCE, NewDeploymentSubsystemProviders.SCANNER_ADD, false);
        registration.registerOperationHandler("remove-scanner", NewDeploymentScannerRemove.INSTANCE, NewDeploymentSubsystemProviders.SCANNER_REMOVE, false);
        registration.registerOperationHandler("enable-scanner", NewDeploymentScannerEnable.INSTANCE, NewDeploymentSubsystemProviders.SCANNER_ENABLE, false);
        registration.registerOperationHandler("disable-scanner", NewDeploymentScannerDisable.INSTANCE, NewDeploymentSubsystemProviders.SCANNER_DISABLE, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser, parser);
    }

    static class DeploymentScannerParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode scanners) throws XMLStreamException {
            for(final Property scanner : scanners.asPropertyList()) {
                final ModelNode node = scanner.getValue();
                writer.writeEmptyElement(Element.DEPLOYMENT_SCANNER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), scanner.getName());
                writer.writeAttribute(Attribute.PATH.getLocalName(), node.get(CommonAttributes.PATH).asString());
                writer.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(), node.get(CommonAttributes.SCAN_ENABLED).asString());
                writer.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(), node.get(CommonAttributes.SCAN_INTERVAL).asString());
                if(node.has(CommonAttributes.RELATIVE_TO)) {
                    writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), node.get(CommonAttributes.RELATIVE_TO).asString());
                }
            }
        }

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            requireNoAttributes(reader);

            // elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DEPLOYMENT_SCANNER_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case DEPLOYMENT_SCANNER: {
                                //noinspection unchecked
                                parseScanner(reader, list);
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

        void parseScanner(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // Handle attributes
            boolean enabled = true;
            int interval = 0;
            String path = null;
            String name = null;
            String relativeTo = null;
            final int attrCount = reader.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (path == null) {
                ParseUtils.missingRequired(reader, Collections.singleton("path"));
            }
            requireNoContent(reader);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("add-deployment-scanner");
            operation.get(OP_ADDR).add(name);
            operation.get(REQUEST_PROPERTIES, CommonAttributes.PATH).set(path);
            operation.get(REQUEST_PROPERTIES, CommonAttributes.SCAN_INTERVAL).set(interval);
            operation.get(REQUEST_PROPERTIES, CommonAttributes.SCAN_ENABLED).set(enabled);
            if(relativeTo != null) operation.get(REQUEST_PROPERTIES, CommonAttributes.RELATIVE_TO).set(relativeTo);
            list.add(operation);
        }

    }

}
