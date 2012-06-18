/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.dmr;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class WSSubsystemReader implements XMLElementReader<List<ModelNode>> {

    private static final WSSubsystemReader INSTANCE = new WSSubsystemReader();

    private WSSubsystemReader() {
        // forbidden instantiation
    }

    static WSSubsystemReader getInstance() {
        return INSTANCE;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);

        final List<ModelNode> endpointConfigs = new ArrayList<ModelNode>();
        final List<ModelNode> clientConfigs = new ArrayList<ModelNode>();

        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element != Element.ENDPOINT_CONFIG && !encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case MODIFY_WSDL_ADDRESS: {
                            boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                            subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                            break;
                        }
                        case WSDL_HOST: {
                            subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
                            break;
                        }
                        case WSDL_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_PORT).set(port);
                            break;
                        }
                        case WSDL_SECURE_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_SECURE_PORT).set(port);
                            break;
                        }
                        case ENDPOINT_CONFIG: {
                            readConfig(reader, subsystem.get(OP_ADDR), endpointConfigs, false);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                case WEBSERVICES_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element != Element.ENDPOINT_CONFIG && element != Element.CLIENT_CONFIG && !encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case MODIFY_WSDL_ADDRESS: {
                            boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                            subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                            break;
                        }
                        case WSDL_HOST: {
                            subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
                            break;
                        }
                        case WSDL_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_PORT).set(port);
                            break;
                        }
                        case WSDL_SECURE_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_SECURE_PORT).set(port);
                            break;
                        }
                        case ENDPOINT_CONFIG: {
                            readConfig(reader, subsystem.get(OP_ADDR), endpointConfigs, false);
                            break;
                        }
                        case CLIENT_CONFIG: {
                            readConfig(reader, subsystem.get(OP_ADDR), clientConfigs, true);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        list.add(subsystem);
        list.addAll(endpointConfigs);
        list.addAll(clientConfigs);
    }

    private String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        return reader.getElementText().trim();
    }

    private void readConfig(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operationList, final boolean client) throws XMLStreamException {
        String configName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                configName = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }

        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(client ? CLIENT_CONFIG : ENDPOINT_CONFIG, configName);
        operationList.add(node);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PRE_HANDLER_CHAIN && element != Element.POST_HANDLER_CHAIN && element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PRE_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configName, operationList, true, client);
                    break;
                }
                case POST_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configName, operationList, false, client);
                    break;
                }
                case PROPERTY : {
                    final ModelNode operation = parseProperty(reader, configName, client);
                    operationList.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseProperty(final XMLExtendedStreamReader reader, final String configName, final boolean client) throws XMLStreamException {
        String propertyName = null;
        String propertyValue = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                propertyName = value;
                break;
            case VALUE:
                propertyValue = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(client ? CLIENT_CONFIG : ENDPOINT_CONFIG, configName).add(PROPERTY, propertyName);
        if (propertyValue != null) {
            operation.get(VALUE).set(propertyValue);
        }
        return operation;
    }

    private void parseHandlerChain(final XMLExtendedStreamReader reader, final String configName,
            final List<ModelNode> operationList, final boolean isPreHandlerChain, final boolean client)
            throws XMLStreamException {
        String handlerChainId = null;
        String protocolBindings = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                handlerChainId = value;
                break;
            case PROTOCOL_BINDINGS:
                protocolBindings = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final List<ModelNode> addHandlerOperations = new LinkedList<ModelNode>();
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case HANDLER: {
                    parseHandler(reader, configName, handlerChainId, isPreHandlerChain, addHandlerOperations, client);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(client ? CLIENT_CONFIG : ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId);
        if (protocolBindings != null) {
            operation.get(Constants.PROTOCOL_BINDINGS).set(protocolBindings);
        }
        operationList.add(operation);
        operationList.addAll(addHandlerOperations);
    }

    private void parseHandler(final XMLExtendedStreamReader reader, final String configName, final String handlerChainId,
            final boolean isPreHandlerChain, final List<ModelNode> operations, final boolean client) throws XMLStreamException {
        String handlerName = null;
        String handlerClass = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                handlerName = value;
                break;
            case CLASS:
                handlerClass = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(client ? CLIENT_CONFIG : ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId).add(HANDLER, handlerName);
        operation.get(CLASS).set(handlerClass);
        operations.add(operation);
    }
}