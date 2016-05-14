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

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
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
@SuppressWarnings({ "deprecation", "static-method" })
public class JGroupsSubsystemXMLReader implements XMLElementReader<List<ModelNode>> {
    private final JGroupsSchema schema;

    JGroupsSubsystemXMLReader(JGroupsSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {

        Map<PathAddress, ModelNode> stackOperations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        stackOperations.put(address, operation);

        if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
            String defaultStack = require(reader, XMLAttribute.DEFAULT_STACK);
            setAttribute(reader, defaultStack, operation, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK);
        }

        Map<PathAddress, ModelNode> channelOperations = new LinkedHashMap<>();

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case CHANNELS: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseChannels(reader, address, channelOperations);
                        break;
                    }
                }
                case STACKS: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseStacks(reader, address, stackOperations, channelOperations);
                        break;
                    }
                }
                case STACK: {
                    if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseStack(reader, address, stackOperations, channelOperations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        // Version prior to 4_0 schema did not require stack being defined,
        // thus iterate over channel add operations and set the stack explicitly.
        if (!this.schema.since(JGroupsSchema.VERSION_4_0)) {
            ModelNode defaultStack = operation.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName());

            for (Map.Entry<PathAddress, ModelNode> entry : channelOperations.entrySet()) {
                PathAddress opAddr = entry.getKey();
                if (opAddr.getLastElement().getKey().equals(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
                    ModelNode op = entry.getValue();
                    if (!op.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                        op.get(ChannelResourceDefinition.Attribute.STACK.getName()).set(defaultStack);
                    }
                }
            }
        }

        // Explicitly order operations such that capabilities are defined before they are referenced
        // This circumvents capability reference integrity issues when operations are not batched
        result.addAll(stackOperations.values());
        result.addAll(channelOperations.values());
    }

    private void parseChannels(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        List<Map.Entry<PathAddress, ModelNode>> tailOperations = new LinkedList<>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    ModelNode defaultChannel = readAttribute(reader, i, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL);
                    ModelNode operation = Operations.createWriteAttributeOperation(address, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL, defaultChannel);
                    tailOperations.add(new AbstractMap.SimpleImmutableEntry<>(address, operation));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
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

        tailOperations.forEach(entry -> operations.put(entry.getKey(), entry.getValue()));
    }

    private void parseChannel(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(ChannelResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.STACK);
                    break;
                }
                case MODULE: {
                    readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.MODULE);
                    break;
                }
                case CLUSTER: {
                    if (this.schema.since(JGroupsSchema.VERSION_4_0)) {
                        readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.CLUSTER);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
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
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = channelAddress.append(ForkResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
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
            XMLElement element = XMLElement.forName(reader.getLocalName());
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

    private void parseStacks(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> stackOperations, Map<PathAddress, ModelNode> channelOperations) throws XMLStreamException {

        ModelNode operation = stackOperations.get(address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    readAttribute(reader, i, operation, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case STACK: {
                    this.parseStack(reader, address, stackOperations, channelOperations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseStack(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> stackOperations, Map<PathAddress, ModelNode> channelOperations) throws XMLStreamException {
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(StackResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        stackOperations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TRANSPORT: {
                    this.parseTransport(reader, address, stackOperations);
                    break;
                }
                case PROTOCOL: {
                    this.parseProtocol(reader, address, stackOperations);
                    break;
                }
                case RELAY: {
                    if (this.schema.since(JGroupsSchema.VERSION_2_0)) {
                        this.parseRelay(reader, address, channelOperations);
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

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(TransportResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SHARED: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.SHARED);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.DIAGNOSTICS_SOCKET_BINDING);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.ThreadingAttribute.DEFAULT_EXECUTOR);
                    break;
                }
                case OOB_EXECUTOR: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.ThreadingAttribute.OOB_EXECUTOR);
                    break;
                }
                case TIMER_EXECUTOR: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.ThreadingAttribute.TIMER_EXECUTOR);
                    break;
                }
                case THREAD_FACTORY: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.ThreadingAttribute.THREAD_FACTORY);
                    break;
                }
                case SITE: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.SITE);
                        break;
                    }
                }
                case RACK: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.RACK);
                        break;
                    }
                }
                case MACHINE: {
                    if (this.schema.since(JGroupsSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.MACHINE);
                        break;
                    }
                }
                default: {
                    this.parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case DEFAULT_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.DEFAULT, reader, address, operations);
                        break;
                    }
                }
                case INTERNAL_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.INTERNAL, reader, address, operations);
                        break;
                    }
                }
                case OOB_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.OOB, reader, address, operations);
                        break;
                    }
                }
                case TIMER_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.TIMER, reader, address, operations);
                        break;
                    }
                }
                default: {
                    this.parseProtocolElement(reader, address, operations);
                }
            }
        }
    }

    private void parseProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
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
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case TYPE: {
                // Already parsed
                break;
            }
            case DATA_SOURCE: {
                readAttribute(reader, index, operation, ProtocolResourceDefinition.Attribute.DATA_SOURCE);
                break;
            }
            case SOCKET_BINDING: {
                readAttribute(reader, index, operation, ProtocolResourceDefinition.Attribute.SOCKET_BINDING);
                break;
            }
            case MODULE: {
                if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                    readAttribute(reader, index, operation, ProtocolResourceDefinition.Attribute.MODULE);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseProtocolElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        XMLElement element = XMLElement.forName(reader.getLocalName());
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

    private void parseProperty(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
        readAttribute(reader, 0, operation, ProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    private void parseThreadPool(ThreadPoolResourceDefinition pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MIN_THREADS:
                    readAttribute(reader, i, operation, pool.getMinThreads());
                    break;
                case MAX_THREADS:
                    readAttribute(reader, i, operation, pool.getMaxThreads());
                    break;
                case QUEUE_LENGTH:
                    readAttribute(reader, i, operation, pool.getQueueLength());
                    break;
                case KEEPALIVE_TIME:
                    readAttribute(reader, i, operation, pool.getKeepAliveTime());
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    readAttribute(reader, i, operation, RelayResourceDefinition.Attribute.SITE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!operation.hasDefined(RelayResourceDefinition.Attribute.SITE.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(XMLAttribute.SITE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
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
        String site = require(reader, XMLAttribute.NAME);
        PathAddress address = relayAddress.append(RemoteSiteResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        String cluster = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, RemoteSiteResourceDefinition.DeprecatedAttribute.STACK);
                    break;
                }
                case CLUSTER: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    cluster = value;
                    break;
                }
                case CHANNEL: {
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        readAttribute(reader, i, operation, RemoteSiteResourceDefinition.Attribute.CHANNEL);

                        // We need to populate the deprecated STACK attribute so that we have enough context for transforming the add operation
                        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
                        PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(value));
                        ModelNode channelOperation = operations.get(channelAddress);
                        if (channelOperation != null) {
                            String stack;
                            if (channelOperation.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                                stack = channelOperation.get(ChannelResourceDefinition.Attribute.STACK.getName()).asString();
                            } else {
                                stack = operations.get(subsystemAddress).get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName()).asString();
                            }
                            setAttribute(reader, stack, operation, RemoteSiteResourceDefinition.DeprecatedAttribute.STACK);
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
            if (!operation.hasDefined(RemoteSiteResourceDefinition.Attribute.CHANNEL.getName())) {
                throw ParseUtils.missingRequired(reader, EnumSet.of(XMLAttribute.CHANNEL));
            }
        } else {
            if (!operation.hasDefined(RemoteSiteResourceDefinition.DeprecatedAttribute.STACK.getName())) {
                throw ParseUtils.missingRequired(reader, EnumSet.of(XMLAttribute.STACK));
            }
            String channel = (cluster != null) ? cluster : site;
            setAttribute(reader, channel, operation, RemoteSiteResourceDefinition.Attribute.CHANNEL);

            // We need to create a corresponding channel add operation
            PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channel));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            String stack = operation.get(RemoteSiteResourceDefinition.DeprecatedAttribute.STACK.getName()).asString();
            setAttribute(reader, stack, channelOperation, ChannelResourceDefinition.Attribute.STACK);
            operations.put(channelAddress, channelOperation);
        }

        ParseUtils.requireNoContent(reader);
    }

    private static String require(XMLExtendedStreamReader reader, XMLAttribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getParser().parseAndSetParameter(attribute.getDefinition(), value, operation, reader);
    }

    private static ModelNode readAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute) throws XMLStreamException {
        return readAttribute(reader, reader.getAttributeValue(index), attribute);
    }

    private static ModelNode readAttribute(XMLExtendedStreamReader reader, String value, Attribute attribute) throws XMLStreamException {
        return attribute.getDefinition().getParser().parse(attribute.getDefinition(), value, reader);
    }
}
