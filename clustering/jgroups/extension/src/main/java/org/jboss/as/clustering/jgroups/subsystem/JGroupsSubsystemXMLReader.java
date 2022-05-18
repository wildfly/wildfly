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
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
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

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case CHANNELS: {
                    this.parseChannels(reader, address, operations);
                    break;
                }
                case STACKS: {
                    this.parseStacks(reader, address, operations);
                    break;
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
                    readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.CLUSTER);
                    break;
                }
                case STATISTICS_ENABLED: {
                    readAttribute(reader, i, operation, ChannelResourceDefinition.Attribute.STATISTICS_ENABLED);
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

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
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
                    readAttribute(reader, i, operation, StackResourceDefinition.Attribute.STATISTICS_ENABLED);
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
                case TRANSPORT: {
                    this.parseTransport(reader, address, operations);
                    break;
                }
                case PROTOCOL: {
                    // If protocol contains socket-binding attribute, parse as a socket-protocol
                    if (reader.getAttributeValue(null, SocketProtocolResourceDefinition.Attribute.SOCKET_BINDING.getDefinition().getXmlName()) != null) {
                        this.parseSocketProtocol(reader, address, operations);
                    } else {
                        this.parseProtocol(reader, address, operations);
                    }
                    break;
                }
                case RELAY: {
                    this.parseRelay(reader, address, operations);
                    break;
                }
                case SOCKET_PROTOCOL: {
                    this.parseSocketProtocol(reader, address, operations);
                    break;
                }
                case SOCKET_DISCOVERY_PROTOCOL: {
                    this.parseSocketDiscoveryProtocol(reader, address, operations);
                    break;
                }
                case JDBC_PROTOCOL: {
                    this.parseJDBCProtocol(reader, address, operations);
                    break;
                }
                case ENCRYPT_PROTOCOL: {
                    this.parseEncryptProtocol(reader, address, operations);
                    break;
                }
                case AUTH_PROTOCOL: {
                    this.parseAuthProtocol(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseTransport(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(TransportResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SHARED: {
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
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
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case OOB_EXECUTOR: {
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case TIMER_EXECUTOR: {
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case THREAD_FACTORY: {
                    if (this.schema.since(JGroupsSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    JGroupsLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case SITE: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.SITE);
                    break;
                }
                case RACK: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.RACK);
                    break;
                }
                case MACHINE: {
                    readAttribute(reader, i, operation, TransportResourceDefinition.Attribute.MACHINE);
                    break;
                }
                case CLIENT_SOCKET_BINDING: {
                    if (this.schema.since(JGroupsSchema.VERSION_7_0)) {
                        readAttribute(reader, i, operation, SocketTransportResourceDefinition.Attribute.CLIENT_SOCKET_BINDING);
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
                    this.parseThreadPool(ThreadPoolResourceDefinition.DEFAULT, reader, address, operations);
                    break;
                }
                default: {
                    this.parseProtocolElement(reader, address, operations);
                }
            }
        }
    }

    private void parseSocketProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(ProtocolResourceDefinition.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SOCKET_BINDING: {
                    readAttribute(reader, i, operation, SocketProtocolResourceDefinition.Attribute.SOCKET_BINDING);
                    break;
                }
                case CLIENT_SOCKET_BINDING: {
                    if (this.schema.since(JGroupsSchema.VERSION_7_0)) {
                        readAttribute(reader, i, operation, SocketProtocolResourceDefinition.Attribute.CLIENT_SOCKET_BINDING);
                        break;
                    }
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        require(reader, operation, SocketProtocolResourceDefinition.Attribute.SOCKET_BINDING);

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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDINGS: {
                    readAttribute(reader, i, operation, SocketDiscoveryProtocolResourceDefinition.Attribute.OUTBOUND_SOCKET_BINDINGS);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATA_SOURCE: {
                    readAttribute(reader, i, operation, JDBCProtocolResourceDefinition.Attribute.DATA_SOURCE);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS: {
                    readAttribute(reader, i, operation, EncryptProtocolResourceDefinition.Attribute.KEY_ALIAS);
                    break;
                }
                case KEY_STORE: {
                    readAttribute(reader, i, operation, EncryptProtocolResourceDefinition.Attribute.KEY_STORE);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
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

    @SuppressWarnings("static-method")
    private void parseProtocolAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case TYPE: {
                // Already parsed
                break;
            }
            case MODULE: {
                readAttribute(reader, index, operation, AbstractProtocolResourceDefinition.Attribute.MODULE);
                break;
            }
            case STATISTICS_ENABLED: {
                readAttribute(reader, index, operation, AbstractProtocolResourceDefinition.Attribute.STATISTICS_ENABLED);
                break;
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

    @SuppressWarnings("static-method")
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

    @SuppressWarnings("static-method")
    private void parseProperty(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
        readElement(reader, operation, AbstractProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    @SuppressWarnings("static-method")
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

    @SuppressWarnings("static-method")
    private void parseRemoteSite(XMLExtendedStreamReader reader, PathAddress relayAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String site = require(reader, XMLAttribute.NAME);
        PathAddress address = relayAddress.append(RemoteSiteResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case CHANNEL: {
                    readAttribute(reader, i, operation, RemoteSiteResourceDefinition.Attribute.CHANNEL);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
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
