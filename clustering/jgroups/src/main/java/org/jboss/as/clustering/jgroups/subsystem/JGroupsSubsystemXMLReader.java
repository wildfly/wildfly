/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * XML reader for the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsSubsystemXMLReader implements XMLElementReader<List<ModelNode>> {
    private final JGroupsSchema schema;

    JGroupsSubsystemXMLReader(JGroupsSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {

        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        String defaultStack = require(reader, Attribute.DEFAULT_STACK);
        JGroupsSubsystemResourceDefinition.DEFAULT_STACK.parseAndSetParameter(defaultStack, operation, reader);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STACK: {
                    this.parseStack(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseStack(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, Attribute.NAME);
        PathAddress address = subsystemAddress.append(StackResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TRANSPORT: {
                    this.parseTransport(reader, address, operations);
                    break;
                }
                case PROTOCOL: {
                    this.parseProtocol(reader, address, operations);
                    break;
                }
                case RELAY: {
                    if (this.schema.since(JGroupsSchema.VERSION_2_0)) {
                        this.parseRelay(reader, address, operations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = stackAddress.append(TransportResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    TransportResourceDefinition.TYPE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SHARED: {
                    TransportResourceDefinition.SHARED.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SOCKET_BINDING: {
                    TransportResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    TransportResourceDefinition.DEFAULT_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case OOB_EXECUTOR: {
                    TransportResourceDefinition.OOB_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TIMER_EXECUTOR: {
                    TransportResourceDefinition.TIMER_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case THREAD_FACTORY: {
                    TransportResourceDefinition.THREAD_FACTORY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SITE: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        TransportResourceDefinition.SITE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case RACK: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        TransportResourceDefinition.RACK.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case MACHINE: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        TransportResourceDefinition.MACHINE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!operation.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    this.parseProperty(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, Attribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createOperation(ModelKeys.ADD_PROTOCOL, stackAddress);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    ProtocolResourceDefinition.TYPE.parseAndSetParameter(type, operation, reader);
                    break;
                }
                case SOCKET_BINDING: {
                    ProtocolResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    this.parseProperty(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseProperty(XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, Attribute.NAME);
        PathAddress address = parentAddress.append(PropertyResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        PropertyResourceDefinition.VALUE.parseAndSetParameter(reader.getElementText(), operation, reader);
    }

    private void parseRelay(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = stackAddress.append(RelayResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    RelayResourceDefinition.SITE.parseAndSetParameter(value, operation, reader);
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

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SITE: {
                    this.parseRemoteSite(reader, address, operations);
                    break;
                }
                case PROPERTY: {
                    this.parseProperty(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseRemoteSite(XMLExtendedStreamReader reader, PathAddress relayAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String site = require(reader, Attribute.NAME);
        PathAddress address = relayAddress.append(RemoteSiteResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    RemoteSiteResourceDefinition.STACK.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case CLUSTER: {
                    RemoteSiteResourceDefinition.CLUSTER.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private static String require(XMLExtendedStreamReader reader, Attribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }
}
