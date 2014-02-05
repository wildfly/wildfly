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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR_REF;
import static org.jboss.as.remoting.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.SECURITY_REALM;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for remoting subsystem 2.0 version
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 * @author Tomaz Cerar
 */
class RemotingSubsystem20Parser extends RemotingSubsystem11Parser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    static final RemotingSubsystem20Parser INSTANCE = new RemotingSubsystem20Parser();
    static final PersistentResourceXMLDescription ENDPOINT_PARSER = PersistentResourceXMLDescription.builder(RemotingEndpointResource.INSTANCE)
            .addAttributes(RemotingEndpointResource.INSTANCE.getAttributes())
            .build();

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

        final PathAddress address = PathAddress.pathAddress(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);

        requireAttributes(reader);
        boolean foundEndpoint = false;
        // Handle elements
        boolean doneWorkerThreadPool = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case WORKER_THREAD_POOL:
                    if (doneWorkerThreadPool) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.WORKER_THREAD_POOL.getLocalName());
                    }
                    if (foundEndpoint) {
                        throw workerThreadPoolEndpointChoiceRequired(reader);
                    }
                    doneWorkerThreadPool = true;
                    parseWorkerThreadPool(reader, subsystem);
                    break;
                case ENDPOINT:
                    if (doneWorkerThreadPool) {
                        throw workerThreadPoolEndpointChoiceRequired(reader);
                    }
                    ENDPOINT_PARSER.parse(reader, PathAddress.pathAddress(address), list);
                    foundEndpoint = true;
                    break;
                case CONNECTOR: {
                    // Add connector updates
                    parseConnector(reader, address.toModelNode(), list);
                    break;
                }
                case HTTP_CONNECTOR: {
                    // Add http connector updates
                    parseHttpConnector(reader, address.toModelNode(), list);
                    break;
                }
                case OUTBOUND_CONNECTIONS: {
                    // parse the outbound-connections
                    this.parseOutboundConnections(reader, address.toModelNode(), list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static XMLStreamException workerThreadPoolEndpointChoiceRequired(XMLExtendedStreamReader reader) {
        return new XMLStreamException(
                RemotingMessages.MESSAGES.workerThreadsEndpointConfigurationChoiceRequired(
                        Element.WORKER_THREAD_POOL.getLocalName(), Element.ENDPOINT.getLocalName()
                ), reader.getLocation());
    }

    void parseRemoteOutboundConnection(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> operations) throws XMLStreamException {
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.OUTBOUND_SOCKET_BINDING_REF);
        final int count = reader.getAttributeCount();
        String name = null;
        final ModelNode addOperation = Util.createAddOperation();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case OUTBOUND_SOCKET_BINDING_REF: {
                    RemoteOutboundConnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF.parseAndSetParameter(value, addOperation, reader);
                    break;
                }
                case USERNAME: {
                    RemoteOutboundConnectionResourceDefinition.USERNAME.parseAndSetParameter(value, addOperation, reader);
                    break;
                }
                case SECURITY_REALM: {
                    RemoteOutboundConnectionResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, addOperation, reader);
                    break;
                }
                case PROTOCOL: {
                    RemoteOutboundConnectionResourceDefinition.PROTOCOL.parseAndSetParameter(value, addOperation, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final PathAddress address = PathAddress.pathAddress(PathAddress.pathAddress(parentAddress), PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION, name));
        addOperation.get(OP_ADDR).set(address.toModelNode());
        // create add operation add it to the list of operations
        operations.add(addOperation);
        // parse the nested elements
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (visited.contains(element)) {
                throw ParseUtils.unexpectedElement(reader);
            }
            visited.add(element);
            switch (element) {
                case PROPERTIES: {
                    parseProperties(reader, address.toModelNode(), operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    void parseHttpConnector(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        String name = null;
        String securityRealm = null;
        String connectorRef = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CONNECTOR_REF);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case SECURITY_REALM: {
                    securityRealm = value;
                    break;
                }
                case CONNECTOR_REF: {
                    connectorRef = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert name != null;
        assert connectorRef != null;

        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        connector.get(OP_ADDR).set(address).add(HTTP_CONNECTOR, name);
        // requestProperties.get(NAME).set(name); // Name is part of the address
        connector.get(CONNECTOR_REF).set(connectorRef);
        if (securityRealm != null) {
            connector.get(SECURITY_REALM).set(securityRealm);
        }
        list.add(connector);
        parseConnectorNestledElements(reader, list, connector);


    }

    private void parseConnectorNestledElements(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode connector) throws XMLStreamException {
        // Handle nested elements.
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (visited.contains(element)) {
                throw unexpectedElement(reader);
            }
            visited.add(element);
            switch (element) {
                case SASL: {
                    parseSaslElement(reader, connector.get(OP_ADDR), list);
                    break;
                }
                case PROPERTIES: {
                    parseProperties(reader, connector.get(OP_ADDR), list);
                    break;
                }
                case AUTHENTICATION_PROVIDER: {
                    connector.get(AUTHENTICATION_PROVIDER).set(readStringAttributeElement(reader, "name"));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

}
