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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 */
public class JGroupsSubsystemXMLReader_2_0 implements XMLElementReader<List<ModelNode>> {
    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH);
        ModelNode subsystem = Util.createAddOperation(subsystemAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_STACK: {
                    JGroupsSubsystemRootResourceDefinition.DEFAULT_STACK.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!subsystem.hasDefined(ModelKeys.DEFAULT_STACK)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DEFAULT_STACK));
        }

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STACK: {
                    this.parseStack(reader, subsystemAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseStack(XMLExtendedStreamReader reader, PathAddress subsystemAddress, List<ModelNode> operations) throws XMLStreamException {
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }
        PathAddress stackAddress = subsystemAddress.append(ModelKeys.STACK, name);
        final ModelNode stack = Util.createAddOperation(stackAddress);
        stack.get(OP_ADDR).set(stackAddress.toModelNode());

        if (!reader.hasNext() || (reader.nextTag() == XMLStreamConstants.END_ELEMENT) || Element.forName(reader.getLocalName()) != Element.TRANSPORT) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.TRANSPORT));
        }
        operations.add(stack);
        this.parseTransport(reader, stackAddress, operations);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROTOCOL: {
                    this.parseProtocol(reader, stackAddress, operations);
                    break;
                }
                case RELAY: {
                    this.parseRelay(reader, stackAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, final PathAddress stackAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress transportAddress = stackAddress.append(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        ModelNode transport = Util.createAddOperation(transportAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        TP.class.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(TP.class).newInstance();
                        TransportResourceDefinition.TYPE.parseAndSetParameter(value, transport, reader);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SHARED: {
                    TransportResourceDefinition.SHARED.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case SOCKET_BINDING: {
                    TransportResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    TransportResourceDefinition.DEFAULT_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case OOB_EXECUTOR: {
                    TransportResourceDefinition.OOB_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case TIMER_EXECUTOR: {
                    TransportResourceDefinition.TIMER_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case THREAD_FACTORY: {
                    TransportResourceDefinition.THREAD_FACTORY.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case SITE: {
                    TransportResourceDefinition.SITE.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case RACK: {
                    TransportResourceDefinition.RACK.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case MACHINE: {
                    TransportResourceDefinition.MACHINE.parseAndSetParameter(value, transport, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!transport.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }
        operations.add(transport);
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    this.parseProperty(reader, transportAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, final List<ModelNode> operations) throws XMLStreamException {

        ModelNode protocol = Util.createOperation(ModelKeys.ADD_PROTOCOL, stackAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        Protocol.class.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(Protocol.class).newInstance();
                        ProtocolResourceDefinition.TYPE.parseAndSetParameter(value, protocol, reader);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SOCKET_BINDING: {
                    ProtocolResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, protocol, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!protocol.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        // in order to add any property, we need the protocol address which will be generated
        PathAddress protocolAddress = stackAddress.append(ModelKeys.PROTOCOL, protocol.get(ModelKeys.TYPE).asString());

        operations.add(protocol);
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    this.parseProperty(reader, protocolAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseProperty(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> operations) throws XMLStreamException {
        String propertyName = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    propertyName = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (propertyName == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        String propertyValue = reader.getElementText();

        // ModelNode for the property add operation
        PathAddress propertyAddress = address.append(ModelKeys.PROPERTY, propertyName);
        ModelNode property = Util.createAddOperation(propertyAddress);

        // assign the value
        PropertyResourceDefinition.VALUE.parseAndSetParameter(propertyValue, property, reader);

        operations.add(property);
    }

    private void parseRelay(XMLExtendedStreamReader reader, final PathAddress stackAddress, List<ModelNode> operations) throws XMLStreamException {
        PathAddress relayAddress = stackAddress.append(ModelKeys.RELAY, ModelKeys.RELAY_NAME);
        ModelNode operation = Util.createAddOperation(relayAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    RelayResource.SITE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!operation.hasDefined(ModelKeys.SITE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.SITE));
        }
        operations.add(operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SITE: {
                    this.parseRemoteSite(reader, relayAddress, operations);
                    break;
                }
                case PROPERTY: {
                    this.parseProperty(reader, relayAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseRemoteSite(XMLExtendedStreamReader reader, final PathAddress relayAddress, List<ModelNode> operations) throws XMLStreamException {
        String site = null;
        ModelNode operation = Util.createAddOperation();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    site = value;
                    break;
                }
                case STACK: {
                    RemoteSiteResource.STACK.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case CLUSTER: {
                    RemoteSiteResource.CLUSTER.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (site == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        ParseUtils.requireNoContent(reader);

        PathAddress siteAddress = relayAddress.append(ModelKeys.REMOTE_SITE, site);
        operation.get(OP_ADDR).set(siteAddress.toModelNode());

        operations.add(operation);
    }
}
