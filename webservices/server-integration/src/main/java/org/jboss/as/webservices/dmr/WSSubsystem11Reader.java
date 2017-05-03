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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
class WSSubsystem11Reader implements XMLElementReader<List<ModelNode>> {

    WSSubsystem11Reader() {

    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(WSExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);
        readAttributes(reader, subsystem);
        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODIFY_WSDL_ADDRESS: {
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    final String value = parseElementNoAttributes(reader);
                    Attributes.MODIFY_WSDL_ADDRESS.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                case WSDL_HOST: {
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    final String value = parseElementNoAttributes(reader);
                    Attributes.WSDL_HOST.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                case WSDL_PORT: {
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    final String value = parseElementNoAttributes(reader);
                    Attributes.WSDL_PORT.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                case WSDL_SECURE_PORT: {
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    final String value = parseElementNoAttributes(reader);
                    Attributes.WSDL_SECURE_PORT.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                case ENDPOINT_CONFIG: {
                    List<ModelNode> configs = readConfig(reader, address, false);
                    list.addAll(configs);
                    break;
                }
                default: {
                    handleUnknownElement(reader, address, element, list, encountered);
                }
            }
        }
        //TODO：check required element
    }

    protected void handleUnknownElement(final XMLExtendedStreamReader reader, final PathAddress parentAddress, Element element, List<ModelNode> list, EnumSet<Element> encountered) throws XMLStreamException {
        throw unexpectedElement(reader);
    }

    protected void readAttributes(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        requireNoAttributes(reader);
    }
    protected String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        return reader.getElementText().trim();
    }

    protected List<ModelNode> readConfig(final XMLExtendedStreamReader reader, final PathAddress parentAddress, final boolean client) throws XMLStreamException {
        final List<ModelNode> configs = new ArrayList<ModelNode>();
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
        final PathAddress address = parentAddress.append(client ? CLIENT_CONFIG : ENDPOINT_CONFIG, configName);
        final ModelNode node = Util.createAddOperation(address);
        configs.add(node);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PRE_HANDLER_CHAIN && element != Element.POST_HANDLER_CHAIN && element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PRE_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configs, true, address);
                    break;
                }
                case POST_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configs, false, address);
                    break;
                }
                case PROPERTY: {
                    final ModelNode operation = parseProperty(reader, address);
                    configs.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return configs;
    }

    private ModelNode parseProperty(final XMLExtendedStreamReader reader, PathAddress parentAddress) throws XMLStreamException {
        final ModelNode operation = Util.createAddOperation(null);
        String propertyName = null;

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
                    Attributes.VALUE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        operation.get(OP_ADDR).set(parentAddress.append(PROPERTY, propertyName).toModelNode());
        return operation;
    }

    private void parseHandlerChain(final XMLExtendedStreamReader reader, final List<ModelNode> operationList, final boolean isPreHandlerChain, PathAddress parentAddress)
            throws XMLStreamException {
        final ModelNode operation = Util.createAddOperation();

        String handlerChainId = null;

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
                    Attributes.PROTOCOL_BINDINGS.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        PathAddress address = parentAddress.append(handlerChainType, handlerChainId);
        operation.get(OP_ADDR).set(address.toModelNode());
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final List<ModelNode> addHandlerOperations = new LinkedList<ModelNode>();
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case HANDLER: {
                    parseHandler(reader, addHandlerOperations, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        operationList.add(operation);
        operationList.addAll(addHandlerOperations);
    }

    private void parseHandler(final XMLExtendedStreamReader reader, final List<ModelNode> operations, PathAddress parentAddress) throws XMLStreamException {
        String handlerName = null;
        final ModelNode operation = Util.createAddOperation();
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
                    Attributes.CLASS.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        operation.get(OP_ADDR).set(parentAddress.append(HANDLER, handlerName).toModelNode());

        operations.add(operation);
    }

}