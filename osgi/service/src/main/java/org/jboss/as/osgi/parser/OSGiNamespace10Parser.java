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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parse subsystem configuration for namespace {@link Namespace#VERSION_1_0}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiNamespace10Parser implements Namespace10, XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    static XMLElementReader<List<ModelNode>> INSTANCE = new OSGiNamespace10Parser();

    // hide ctor
    private OSGiNamespace10Parser() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode activation = parseActivationAttribute(reader, address);
        operations.add(activation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONFIGURATION: {
                            parseConfigurations(reader);
                            break;
                        }
                        case PROPERTIES: {
                            List<ModelNode> result = parseFrameworkProperties(reader, address, operations);
                            operations.addAll(result);
                            break;
                        }
                        case MODULES: {
                            List<ModelNode> result = parseModules(reader, address);
                            operations.addAll(result);
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

    private ModelNode parseActivationAttribute(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {
        final ModelNode result = new ModelNode();
        result.get(OP).set(ADD);
        result.get(OP_ADDR).set(address);
        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case VERSION_1_0: {
                // Handle attributes
                int count = reader.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    requireNoNamespaceAttribute(reader, i);
                    final String attrValue = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case ACTIVATION: {
                            OSGiRootResource.ACTIVATION.parseAndSetParameter(attrValue, result, reader);
                            break;
                        }
                        default:
                            throw unexpectedAttribute(reader, i);
                    }
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
        }
        return result;
    }

    private void parseConfigurations(XMLExtendedStreamReader reader) throws XMLStreamException {

        // Handle attributes
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final Namespace11.Attribute attribute = Namespace11.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PID: {
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        reader.getElementText().trim();
                        break;
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private List<ModelNode> parseFrameworkProperties(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operations) throws XMLStreamException {

        requireNoAttributes(reader);

        List<ModelNode> result = new ArrayList<ModelNode>();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        String name = null;
                        String value = null;
                        int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String attrValue = reader.getAttributeValue(i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = attrValue;
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (name == null) {
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }
                        value = reader.getElementText().trim();

                        ModelNode propNode = new ModelNode();
                        propNode.get(OP).set(ADD);
                        propNode.get(OP_ADDR).set(address).add(ModelConstants.PROPERTY, name);
                        FrameworkPropertyResource.VALUE.parseAndSetParameter(value, propNode, reader);

                        result.add(propNode);
                        break;
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        return result;
    }

    private List<ModelNode> parseModules(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {
        List<ModelNode> nodes = new ArrayList<ModelNode>();
        requireNoAttributes(reader);

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.MODULE) {
                        final int count = reader.getAttributeCount();

                        ModelNode moduleNode = new ModelNode();
                        moduleNode.get(OP).set(ADD);

                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case IDENTIFIER: {
                                    moduleNode.get(OP_ADDR).set(address).add(ModelConstants.CAPABILITY, reader.getAttributeValue(i));
                                    break;
                                }
                                case STARTLEVEL: {
                                    FrameworkCapabilityResource.STARTLEVEL.parseAndSetParameter(reader.getAttributeValue(i), moduleNode, reader);
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (!moduleNode.hasDefined(OP_ADDR)) {
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }

                        nodes.add(moduleNode);

                        requireNoContent(reader);
                        break;
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        return nodes;
    }
}