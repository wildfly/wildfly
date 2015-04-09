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

        if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
            @SuppressWarnings("deprecation")
            String defaultStack = require(reader, Attribute.DEFAULT_STACK);
            JGroupsSubsystemResourceDefinition.DEFAULT_STACK.parseAndSetParameter(defaultStack, operation, reader);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CHANNELS: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseChannels(reader, address, operations);
                        break;
                    }
                }
                case STACKS: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseStacks(reader, address, operations);
                        break;
                    }
                }
                case STACK: {
                    if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseStack(reader, address, operations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseChannels(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        ModelNode operation = operations.get(address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.parseAndSetParameter(value, operation, reader);
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
                case CHANNEL: {
                    this.parseChannel(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseChannel(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, Attribute.NAME);
        PathAddress address = subsystemAddress.append(ChannelResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    ChannelResourceDefinition.STACK.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MODULE: {
                    ChannelResourceDefinition.MODULE.parseAndSetParameter(value, operation, reader);
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
                case FORK: {
                    this.parseFork(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseFork(XMLExtendedStreamReader reader, PathAddress channelAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, Attribute.NAME);
        PathAddress address = channelAddress.append(ForkResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
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
                case PROTOCOL: {
                    this.parseProtocol(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseStacks(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        ModelNode operation = operations.get(address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    JGroupsSubsystemResourceDefinition.DEFAULT_STACK.parseAndSetParameter(value, operation, reader);
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
                case STACK: {
                    this.parseStack(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
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

        String type = require(reader, Attribute.TYPE);
        PathAddress address = stackAddress.append(TransportResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SHARED: {
                    TransportResourceDefinition.SHARED.parseAndSetParameter(value, operation, reader);
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
                    this.parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, Attribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseProtocolAttribute(reader, i, operation);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        String value = reader.getAttributeValue(index);
        Attribute attribute = Attribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case TYPE: {
                // Already parsed
                break;
            }
            case SOCKET_BINDING: {
                ProtocolResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, operation, reader);
                break;
            }
            case MODULE: {
                if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                    ProtocolResourceDefinition.MODULE.parseAndSetParameter(value, operation, reader);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseProtocolElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case PROPERTY: {
                this.parseProperty(reader, address, operations);
                break;
            }
            case DEFAULT_THREAD_POOL:
                parseThreadPool(ThreadPoolResourceDefinition.DEFAULT, reader, address, operations);
                break;
            case INTERNAL_THREAD_POOL:
                parseThreadPool(ThreadPoolResourceDefinition.INTERNAL, reader, address, operations);
                break;
            case OOB_THREAD_POOL:
                parseThreadPool(ThreadPoolResourceDefinition.OOB, reader, address, operations);
                break;
            case TIMER_THREAD_POOL:
                parseThreadPool(ThreadPoolResourceDefinition.TIMER, reader, address, operations);
                break;
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseProperty(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        String name = require(reader, Attribute.NAME);
        ProtocolResourceDefinition.PROPERTIES.parseAndAddParameterElement(name, reader.getElementText(), operation, reader);
    }

    private void parseThreadPool(ThreadPoolResourceDefinition pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MIN_THREADS:
                    pool.getMinThreads().parseAndSetParameter(value, operation, reader);
                    break;
                case MAX_THREADS:
                    pool.getMaxThreads().parseAndSetParameter(value, operation, reader);
                    break;
                case QUEUE_LENGTH:
                    pool.getQueueLength().parseAndSetParameter(value, operation, reader);
                    break;
                case KEEPALIVE_TIME:
                    pool.getKeepaliveTime().parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        ParseUtils.requireNoContent(reader);
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

        if (!operation.hasDefined(RelayResourceDefinition.SITE.getName())) {
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

        String cluster = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        RemoteSiteResourceDefinition.STACK.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case CLUSTER: {
                    if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        cluster = value;
                        break;
                    }
                }
                case CHANNEL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        RemoteSiteResourceDefinition.CHANNEL.parseAndSetParameter(value, operation, reader);

                        // We need to populate the deprecated STACK attribute so that we have enough context for transforming the add operation
                        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
                        PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(value));
                        ModelNode channelOperation = operations.get(channelAddress);
                        if (channelOperation != null) {
                            String stack;
                            if (channelOperation.hasDefined(ChannelResourceDefinition.STACK.getName())) {
                                stack = channelOperation.get(ChannelResourceDefinition.STACK.getName()).asString();
                            } else {
                                stack = operations.get(subsystemAddress).get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName()).asString();
                            }
                            RemoteSiteResourceDefinition.STACK.parseAndSetParameter(stack, operation, reader);
                        }
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
            if (!operation.hasDefined(RemoteSiteResourceDefinition.CHANNEL.getName())) {
                throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.CHANNEL));
            }
        } else {
            if (!operation.hasDefined(RemoteSiteResourceDefinition.STACK.getName())) {
                throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.STACK));
            }
            String channel = (cluster != null) ? cluster : site;
            RemoteSiteResourceDefinition.CHANNEL.parseAndSetParameter(channel, operation, reader);

            // We need to create a corresponding channel add operation
            PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channel));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            String stack = operation.get(RemoteSiteResourceDefinition.STACK.getName()).asString();
            ChannelResourceDefinition.STACK.parseAndSetParameter(stack, channelOperation, reader);
            operations.put(channelAddress, channelOperation);
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
