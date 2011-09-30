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
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CAPABILITY;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.ENTRIES;
import static org.jboss.as.osgi.parser.CommonAttributes.FRAMEWORK_PROPERTY;
import static org.jboss.as.osgi.parser.CommonAttributes.STARTLEVEL;
import static org.jboss.as.osgi.parser.CommonAttributes.VALUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Parse subsystem configuration.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode activation = parseActivationAttribute(reader, address);
        operations.add(activation);

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONFIGURATION: {
                            List<ModelNode> result = parseConfigurationElement(reader, address);
                            operations.addAll(result);
                            break;
                        }
                        case PROPERTIES: {
                            List<ModelNode> result = parsePropertiesElement(reader, address, operations);
                            operations.addAll(result);
                            break;
                        }
                        case CAPABILITIES: {
                            List<ModelNode> result = parseModulesElement(reader, address);
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
            case OSGI_1_0: {
                // Handle attributes
                int count = reader.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    requireNoNamespaceAttribute(reader, i);
                    final String attrValue = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case ACTIVATION: {
                            result.get(ACTIVATION).set(attrValue);
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

    List<ModelNode> parseConfigurationElement(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        // Handle attributes
        String pid = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PID: {
                    pid = attrValue;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (pid == null)
            throw missingRequired(reader, Collections.singleton(Attribute.PID));

        ModelNode configuration = new ModelNode();
        configuration.get(OP).set(ADD);
        configuration.get(OP_ADDR).set(address).add(CONFIGURATION, pid);

        List<ModelNode> result = new ArrayList<ModelNode>();
        result.add(configuration);

        ModelNode propNode = configuration.get(ENTRIES);

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        // Handle attributes
                        String name = null;
                        String value = null;
                        count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String attrValue = reader.getAttributeValue(i);

                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = attrValue;
                                    //if (configuration.has(name))
                                    //    throw new XMLStreamException(MESSAGES.propertyAlreadyExists(name), reader.getLocation());
                                    break;
                                }
                                case VALUE: {
                                    value = attrValue;
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (name == null)
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                        if (value == null)
                            throw missingRequired(reader, Collections.singleton(Attribute.VALUE));

                        requireNoContent(reader);

                        propNode.get(name).set(value);

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

    List<ModelNode> parsePropertiesElement(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operations) throws XMLStreamException {

        requireNoAttributes(reader);

        List<ModelNode> result = new ArrayList<ModelNode>();

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
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
                        propNode.get(OP_ADDR).set(address).add(FRAMEWORK_PROPERTY, name);
                        propNode.get(VALUE).set(value);

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

    List<ModelNode> parseModulesElement(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {
        List<ModelNode> nodes = new ArrayList<ModelNode>();
        requireNoAttributes(reader);

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.CAPABILITY) {
                        String name = null;
                        String start = null;
                        final int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = reader.getAttributeValue(i);
                                    break;
                                }
                                case STARTLEVEL: {
                                    start = reader.getAttributeValue(i);
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (name == null)
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));

                        ModelNode moduleNode = new ModelNode();
                        moduleNode.get(OP).set(ADD);
                        moduleNode.get(OP_ADDR).set(address).add(CAPABILITY, name);
                        if (start != null)
                            moduleNode.get(STARTLEVEL).set(start);

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

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (has(node, ACTIVATION)) {
            writeAttribute(writer, Attribute.ACTIVATION, node.get(ACTIVATION));
        }

        if (has(node, CONFIGURATION)) {
            ModelNode configuration = node.get(CONFIGURATION);
            for (String pid : new TreeSet<String>(configuration.keys())) {
                writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                writer.writeAttribute(Attribute.PID.getLocalName(), pid);

                ModelNode entries = configuration.get(pid).get(ENTRIES);
                if (entries.isDefined()) {
                    for (String propKey : entries.keys()) {
                        String propValue = entries.get(propKey).asString();
                        writer.writeStartElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), propKey);
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), propValue);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }
        }

        if (has(node, FRAMEWORK_PROPERTY)) {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            ModelNode properties = node.get(FRAMEWORK_PROPERTY);
            for (String key : new TreeSet<String>(properties.keys())) {
                String val = properties.get(key).get(VALUE).asString();
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                writer.writeCharacters(val);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (has(node, CAPABILITY)) {
            writer.writeStartElement(Element.CAPABILITIES.getLocalName());
            ModelNode modules = node.get(CAPABILITY);
            for (String key : modules.keys()) {
                ModelNode moduleNode = modules.get(key);
                writer.writeEmptyElement(Element.CAPABILITY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                if (moduleNode.has(STARTLEVEL)) {
                    writeAttribute(writer, Attribute.STARTLEVEL, moduleNode.require(STARTLEVEL));
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private boolean has(ModelNode node, String name) {
        if (node.has(name) && node.get(name).isDefined()) {
            ModelNode n = node.get(name);
            switch (n.getType()) {
                case LIST:
                case OBJECT:
                    return n.asList().size() > 0;
                default:
                    return true;
            }
        }
        return false;
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }
}