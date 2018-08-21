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
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
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

        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        if (!this.schema.since(JGroupsSchema.VERSION_3_0)) {
            String defaultStack = require(reader, XMLAttribute.DEFAULT_STACK);
            setAttribute(reader, defaultStack, operation, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
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

        // Version prior to 4_0 schema did not require stack being defined,
        // thus iterate over channel add operations and set the stack explicitly.
        if (!this.schema.since(JGroupsSchema.VERSION_4_0)) {
            ModelNode defaultStack = operation.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName());

            for (Map.Entry<PathAddress, ModelNode> entry : operations.entrySet()) {
                PathAddress opAddr = entry.getKey();
                if (opAddr.getLastElement().getKey().equals(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
                    ModelNode op = entry.getValue();
                    if (!op.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                        op.get(ChannelResourceDefinition.Attribute.STACK.getName()).set(defaultStack);
                    }
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseChannels(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        ModelNode operation = operations.get(address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    readAttribute(reader, i, operation, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL);
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
                case STATISTICS_ENABLED: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.STATISTICS_ENABLED);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (this.schema.since(JGroupsSchema.VERSION_4_0)) {
            require(reader, operation, ChannelResourceDefinition.Attribute.STACK);
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

    private void parseStacks(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        ModelNode operation = operations.get(address);

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
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(StackResourceDefinition.pathElement(name));
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
                case STATISTICS_ENABLED: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        readAttribute(reader, i, operation, StackResourceDefinition.Attribute.STATISTICS_ENABLED);
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
                case SOCKET_PROTOCOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        this.parseSocketProtocol(reader, address, operations);
                        break;
                    }
                }
                case SOCKET_DISCOVERY_PROTOCOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        this.parseSocketDiscoveryProtocol(reader, address, operations);
                        break;
                    }
                }
                case JDBC_PROTOCOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        this.parseJDBCProtocol(reader, address, operations);
                        break;
                    }
                }
                case ENCRYPT_PROTOCOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        this.parseEncryptProtocol(reader, address, operations);
                        break;
                    }
                }
                case AUTH_PROTOCOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                        this.parseAuthProtocol(reader, address, operations);
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
                case SOCKET_BINDING: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.SOCKET_BINDING);
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
                    if (this.schema.since(JGroupsSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.INTERNAL, reader, address, operations);
                        break;
                    }
                }
                case OOB_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.OOB, reader, address, operations);
                        break;
                    }
                }
                case TIMER_THREAD_POOL: {
                    if (this.schema.since(JGroupsSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
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

        // Set default port_range for pre-WF11 schemas
        if (!this.schema.since(JGroupsSchema.VERSION_5_0)) {
            String portRangeProperty = "port_range";
            if (!operation.hasDefined(AbstractProtocolResourceDefinition.Attribute.PROPERTIES.getName(), portRangeProperty)) {
                operation.get(AbstractProtocolResourceDefinition.Attribute.PROPERTIES.getName()).get(portRangeProperty).set("50");
            }
        }
    }

    private void parseSocketProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseSocketProtocolAttribute(reader, i, operation);
        }

        require(reader, operation, SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseSocketDiscoveryProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseSocketDiscoveryProtocolAttribute(reader, i, operation);
        }

        require(reader, operation, SocketDiscoveryProtocolResourceDefinition.Attribute.OUTBOUND_SOCKET_BINDINGS);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseJDBCProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseJDBCProtocolAttribute(reader, i, operation);
        }

        require(reader, operation, JDBCProtocolResourceDefinition.Attribute.DATA_SOURCE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseEncryptProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseEncryptProtocolAttribute(reader, i, operation);
        }

        require(reader, operation, EncryptProtocolResourceDefinition.Attribute.KEY_ALIAS, EncryptProtocolResourceDefinition.Attribute.KEY_STORE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseEncryptProtocolElement(reader, address, operations);
        }
    }

    private void parseAuthProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseProtocolAttribute(reader, i, operation);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseAuthProtocolElement(reader, address, operations);
        }

        if (!operations.containsKey(address.append(AuthTokenResourceDefinition.WILDCARD_PATH))) {
            throw ParseUtils.missingOneOf(reader, EnumSet.of(XMLElement.PLAIN_TOKEN, XMLElement.DIGEST_TOKEN, XMLElement.CIPHER_TOKEN));
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

    private void parseSocketProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SOCKET_BINDING: {
                readAttribute(reader, index, operation, SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING);
                break;
            }
            default: {
                parseProtocolAttribute(reader, index, operation);
            }
        }
    }

    private void parseSocketDiscoveryProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case OUTBOUND_SOCKET_BINDINGS: {
                readAttribute(reader, index, operation, SocketDiscoveryProtocolResourceDefinition.Attribute.OUTBOUND_SOCKET_BINDINGS);
                break;
            }
            default: {
                parseProtocolAttribute(reader, index, operation);
            }
        }
    }

    private void parseJDBCProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case DATA_SOURCE: {
                readAttribute(reader, index, operation, JDBCProtocolResourceDefinition.Attribute.DATA_SOURCE);
                break;
            }
            default: {
                parseProtocolAttribute(reader, index, operation);
            }
        }
    }

    private void parseEncryptProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case KEY_ALIAS: {
                readAttribute(reader, index, operation, EncryptProtocolResourceDefinition.Attribute.KEY_ALIAS);
                break;
            }
            case KEY_STORE: {
                readAttribute(reader, index, operation, EncryptProtocolResourceDefinition.Attribute.KEY_STORE);
                break;
            }
            default: {
                parseProtocolAttribute(reader, index, operation);
            }
        }
    }

    private void parseProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case TYPE: {
                // Already parsed
                break;
            }
            case SOCKET_BINDING: {
                String protocol = Operations.getPathAddress(operation).getLastElement().getValue();
                Attribute socketBindingAttribute = GenericProtocolResourceDefinition.DeprecatedAttribute.SOCKET_BINDING;
                for (ProtocolRegistration.MulticastProtocol multicastProtocol : EnumSet.allOf(ProtocolRegistration.MulticastProtocol.class)) {
                    if (protocol.equals(multicastProtocol.name())) {
                        socketBindingAttribute = SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING;
                        break;
                    }
                }
                readAttribute(reader, index, operation, socketBindingAttribute);
                break;
            }
            case DATA_SOURCE: {
                if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                readAttribute(reader, index, operation, JDBCProtocolResourceDefinition.Attribute.DATA_SOURCE);
                break;
            }
            case MODULE: {
                if (this.schema.since(JGroupsSchema.VERSION_3_0)) {
                    readAttribute(reader, index, operation, AbstractProtocolResourceDefinition.Attribute.MODULE);
                    break;
                }
            }
            case STATISTICS_ENABLED: {
                if (this.schema.since(JGroupsSchema.VERSION_5_0)) {
                    readAttribute(reader, index, operation, AbstractProtocolResourceDefinition.Attribute.STATISTICS_ENABLED);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseEncryptProtocolElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case KEY_CREDENTIAL_REFERENCE: {
                readElement(reader, operation, EncryptProtocolResourceDefinition.Attribute.KEY_CREDENTIAL);
                break;
            }
            default: {
                this.parseProtocolElement(reader, address, operations);
            }
        }
    }

    private void parseAuthProtocolElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case PLAIN_TOKEN: {
                this.parsePlainAuthToken(reader, address, operations);
                break;
            }
            case DIGEST_TOKEN: {
                this.parseDigestAuthToken(reader, address, operations);
                break;
            }
            case CIPHER_TOKEN: {
                this.parseCipherAuthToken(reader, address, operations);
                break;
            }
            default: {
                this.parseProtocolElement(reader, address, operations);
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

    private void parsePlainAuthToken(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = protocolAddress.append(PlainAuthTokenResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinition.WILDCARD_PATH), operation);

        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseAuthTokenElement(reader, protocolAddress, operations);
        }

        require(reader, operation, AuthTokenResourceDefinition.Attribute.SHARED_SECRET);
    }

    private void parseDigestAuthToken(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = protocolAddress.append(DigestAuthTokenResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinition.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ALGORITHM: {
                    readAttribute(reader, i, operation, DigestAuthTokenResourceDefinition.Attribute.ALGORITHM);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseAuthTokenElement(reader, protocolAddress, operations);
        }

        require(reader, operation, AuthTokenResourceDefinition.Attribute.SHARED_SECRET);
    }

    private void parseCipherAuthToken(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = protocolAddress.append(CipherAuthTokenResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinition.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinition.Attribute.KEY_ALIAS);
                    break;
                }
                case KEY_STORE: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinition.Attribute.KEY_STORE);
                    break;
                }
                case ALGORITHM: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinition.Attribute.ALGORITHM);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        require(reader, operation, CipherAuthTokenResourceDefinition.Attribute.KEY_ALIAS, CipherAuthTokenResourceDefinition.Attribute.KEY_STORE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case KEY_CREDENTIAL_REFERENCE: {
                    readElement(reader, operation, CipherAuthTokenResourceDefinition.Attribute.KEY_CREDENTIAL);
                    break;
                }
                default: {
                    this.parseAuthTokenElement(reader, protocolAddress, operations);
                }
            }
        }

        require(reader, operation, CipherAuthTokenResourceDefinition.Attribute.KEY_CREDENTIAL, AuthTokenResourceDefinition.Attribute.SHARED_SECRET);
    }

    private void parseAuthTokenElement(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(protocolAddress.append(AuthTokenResourceDefinition.WILDCARD_PATH));
        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case SHARED_SECRET_CREDENTIAL_REFERENCE: {
                readElement(reader, operation, AuthTokenResourceDefinition.Attribute.SHARED_SECRET);
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
        readElement(reader, operation, AbstractProtocolResourceDefinition.Attribute.PROPERTIES);
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
                    if (this.schema.since(JGroupsSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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

        require(reader, operation, RelayResourceDefinition.Attribute.SITE);

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
            require(reader, operation, RemoteSiteResourceDefinition.Attribute.CHANNEL);
        } else {
            require(reader, operation, RemoteSiteResourceDefinition.DeprecatedAttribute.STACK);
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

    private static void require(XMLExtendedStreamReader reader, ModelNode operation, Attribute... attributes) throws XMLStreamException {
        for (Attribute attribute : attributes) {
            if (!operation.hasDefined(attribute.getName())) {
                AttributeDefinition definition = attribute.getDefinition();
                Set<String> names = Collections.singleton(definition.getXmlName());
                throw definition.getParser().isParseAsElement() ? ParseUtils.missingRequiredElement(reader, names) : ParseUtils.missingRequired(reader, names);
            }
        }
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getParser().parseAndSetParameter(attribute.getDefinition(), value, operation, reader);
    }

    private static void readElement(XMLExtendedStreamReader reader, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        AttributeParser parser = definition.getParser();
        if (parser.isParseAsElement()) {
            parser.parseElement(definition, reader, operation);
        } else {
            parser.parseAndSetParameter(definition, reader.getElementText(), operation, reader);
        }
    }
}
