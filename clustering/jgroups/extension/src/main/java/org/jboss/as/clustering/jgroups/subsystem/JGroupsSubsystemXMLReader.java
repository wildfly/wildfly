/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import org.jboss.as.clustering.logging.ClusteringLogger;
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
    private final JGroupsSubsystemSchema schema;

    JGroupsSubsystemXMLReader(JGroupsSubsystemSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {

        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        String defaultStack = null;

        if (!this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
            defaultStack = require(reader, XMLAttribute.DEFAULT_STACK);
            // Fabricate a default channel
            PathAddress channelAddress = address.append(ChannelResourceDefinitionRegistrar.pathElement("ee"));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            channelOperation.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(defaultStack);
            channelOperation.get(ChannelResourceDefinitionRegistrar.CLUSTER.getName()).set("ejb");
            operations.put(channelAddress, channelOperation);
            operation.get(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL.getName()).set(channelAddress.getLastElement().getValue());
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case CHANNELS: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        this.parseChannels(reader, address, operations);
                        break;
                    }
                }
                case STACKS: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        defaultStack = this.parseStacks(reader, address, operations, defaultStack);
                        break;
                    }
                }
                case STACK: {
                    if (!this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        this.parseStack(reader, address, operations, defaultStack);
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
        if (!this.schema.since(JGroupsSubsystemSchema.VERSION_4_0)) {
            for (Map.Entry<PathAddress, ModelNode> entry : operations.entrySet()) {
                PathAddress opAddr = entry.getKey();
                if (opAddr.getLastElement().getKey().equals(ChannelResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                    ModelNode op = entry.getValue();
                    if (!op.hasDefined(ChannelResourceDefinitionRegistrar.STACK.getName())) {
                        op.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(defaultStack);
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
                    readAttribute(reader, i, operation, JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL);
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
        PathAddress address = subsystemAddress.append(ChannelResourceDefinitionRegistrar.pathElement(name));
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
                    readAttribute(reader, i, operation, ChannelResourceDefinitionRegistrar.STACK);
                    break;
                }
                case MODULE: {
                    readAttribute(reader, i, operation, ChannelResourceDefinitionRegistrar.MODULE);
                    break;
                }
                case CLUSTER: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_4_0)) {
                        readAttribute(reader, i, operation, ChannelResourceDefinitionRegistrar.CLUSTER);
                        break;
                    }
                }
                case STATISTICS_ENABLED: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        readAttribute(reader, i, operation, ChannelResourceDefinitionRegistrar.STATISTICS_ENABLED);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (this.schema.since(JGroupsSubsystemSchema.VERSION_4_0)) {
            require(reader, operation, ChannelResourceDefinitionRegistrar.STACK);
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
        PathAddress address = channelAddress.append(ForkResourceDefinitionRegistrar.pathElement(name));
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

    private String parseStacks(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations, String legacyDefaultStack) throws XMLStreamException {

        String defaultStack = legacyDefaultStack;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    defaultStack = reader.getAttributeValue(i);
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
                    this.parseStack(reader, address, operations, defaultStack);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return defaultStack;
    }

    private void parseStack(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations, String defaultStack) throws XMLStreamException {
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(StackResourceDefinitionRegistrar.pathElement(name));
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
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        readAttribute(reader, i, operation, StackResourceDefinitionRegistrar.STATISTICS_ENABLED);
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
                    // If protocol contains socket-binding attribute, parse as a socket-protocol
                    if (reader.getAttributeValue(null, SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get().getXmlName()) != null) {
                        this.parseSocketProtocol(reader, address, operations);
                    } else {
                        this.parseProtocol(reader, address, operations);
                    }
                    break;
                }
                case RELAY: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_2_0)) {
                        this.parseRelay(reader, address, operations, defaultStack);
                        break;
                    }
                }
                case SOCKET_PROTOCOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        this.parseSocketProtocol(reader, address, operations);
                        break;
                    }
                }
                case SOCKET_DISCOVERY_PROTOCOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        this.parseSocketDiscoveryProtocol(reader, address, operations);
                        break;
                    }
                }
                case JDBC_PROTOCOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        this.parseJDBCProtocol(reader, address, operations);
                        break;
                    }
                }
                case ENCRYPT_PROTOCOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                        this.parseEncryptProtocol(reader, address, operations);
                        break;
                    }
                }
                case AUTH_PROTOCOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
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
        PathAddress address = stackAddress.append(AbstractTransportResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SOCKET_BINDING: {
                    readAttribute(reader, i, operation, AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.TRANSPORT.get());
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    readAttribute(reader, i, operation, AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.DIAGNOSTICS.get());
                    break;
                }
                case SHARED:
                case DEFAULT_EXECUTOR:
                case OOB_EXECUTOR:
                case TIMER_EXECUTOR:
                case THREAD_FACTORY:
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_9_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ClusteringLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                case SITE: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, AbstractTransportResourceDefinitionRegistrar.Attribute.SITE.get());
                        break;
                    }
                }
                case RACK: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, AbstractTransportResourceDefinitionRegistrar.Attribute.RACK.get());
                        break;
                    }
                }
                case MACHINE: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, AbstractTransportResourceDefinitionRegistrar.Attribute.MACHINE.get());
                        break;
                    }
                }
                case CLIENT_SOCKET_BINDING: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_7_0)) {
                        readAttribute(reader, i, operation, SocketTransportResourceDefinitionRegistrar.CLIENT_SOCKET_BINDING);
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
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinitionRegistrar.DEFAULT, reader, address, operations);
                        break;
                    }
                }
                case INTERNAL_THREAD_POOL:
                case OOB_THREAD_POOL:
                case TIMER_THREAD_POOL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        ParseUtils.requireNoContent(reader);
                        break;
                    }
                }
                default: {
                    this.parseProtocolElement(reader, address, operations);
                }
            }
        }

        // Set default port_range for pre-WF11 schemas
        if (!this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
            String portRangeProperty = "port_range";
            if (!operation.hasDefined(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), portRangeProperty)) {
                operation.get(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName()).get(portRangeProperty).set("50");
            }
        }
    }

    private void parseSocketProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SOCKET_BINDING: {
                    readAttribute(reader, i, operation, SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get());
                    break;
                }
                case CLIENT_SOCKET_BINDING: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_7_0)) {
                        readAttribute(reader, i, operation, SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.CLIENT.get());
                        break;
                    }
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        require(reader, operation, SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get());

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseSocketDiscoveryProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDINGS: {
                    readAttribute(reader, i, operation, SocketDiscoveryProtocolResourceDefinitionRegistrar.OUTBOUND_SOCKET_BINDINGS);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        require(reader, operation, SocketDiscoveryProtocolResourceDefinitionRegistrar.OUTBOUND_SOCKET_BINDINGS);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseJDBCProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATA_SOURCE: {
                    readAttribute(reader, i, operation, JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        require(reader, operation, JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseProtocolElement(reader, address, operations);
        }
    }

    private void parseEncryptProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS: {
                    readAttribute(reader, i, operation, EncryptProtocolResourceDefinitionRegistrar.KEY_ALIAS);
                    break;
                }
                case KEY_STORE: {
                    readAttribute(reader, i, operation, EncryptProtocolResourceDefinitionRegistrar.KEY_STORE);
                    break;
                }
                default: {
                    parseProtocolAttribute(reader, i, operation);
                }
            }
        }

        require(reader, operation, EncryptProtocolResourceDefinitionRegistrar.KEY_ALIAS, EncryptProtocolResourceDefinitionRegistrar.KEY_STORE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseEncryptProtocolElement(reader, address, operations);
        }
    }

    private void parseAuthProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseProtocolAttribute(reader, i, operation);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseAuthProtocolElement(reader, address, operations);
        }

        if (!operations.containsKey(address.append(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH))) {
            throw ParseUtils.missingOneOf(reader, EnumSet.of(XMLElement.PLAIN_TOKEN, XMLElement.DIGEST_TOKEN, XMLElement.CIPHER_TOKEN));
        }
    }

    private void parseProtocol(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String type = require(reader, XMLAttribute.TYPE);
        PathAddress address = stackAddress.append(AbstractProtocolResourceDefinitionRegistrar.pathElement(type));
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
                if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                readAttribute(reader, index, operation, JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);
                break;
            }
            case MODULE: {
                if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                    readAttribute(reader, index, operation, ProtocolChildResourceDefinitionRegistrar.MODULE);
                    break;
                }
            }
            case STATISTICS_ENABLED: {
                if (this.schema.since(JGroupsSubsystemSchema.VERSION_5_0)) {
                    readAttribute(reader, index, operation, ProtocolChildResourceDefinitionRegistrar.STATISTICS_ENABLED);
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
                readElement(reader, operation, EncryptProtocolResourceDefinitionRegistrar.KEY_CREDENTIAL);
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
        PathAddress address = protocolAddress.append(PlainAuthTokenResourceDefinitionRegistrar.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH), operation);

        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseAuthTokenElement(reader, protocolAddress, operations);
        }

        require(reader, operation, AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
    }

    private void parseDigestAuthToken(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = protocolAddress.append(DigestAuthTokenResourceDefinitionRegistrar.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ALGORITHM: {
                    readAttribute(reader, i, operation, DigestAuthTokenResourceDefinitionRegistrar.Attribute.ALGORITHM.get());
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

        require(reader, operation, AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
    }

    private void parseCipherAuthToken(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = protocolAddress.append(CipherAuthTokenResourceDefinitionRegistrar.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(protocolAddress.append(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEY_ALIAS: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinitionRegistrar.Attribute.KEY_ALIAS.get());
                    break;
                }
                case KEY_STORE: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinitionRegistrar.KEY_STORE);
                    break;
                }
                case ALGORITHM: {
                    readAttribute(reader, i, operation, CipherAuthTokenResourceDefinitionRegistrar.Attribute.ALGORITHM.get());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        require(reader, operation, CipherAuthTokenResourceDefinitionRegistrar.Attribute.KEY_ALIAS.get(), CipherAuthTokenResourceDefinitionRegistrar.KEY_STORE);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case KEY_CREDENTIAL_REFERENCE: {
                    readElement(reader, operation, CipherAuthTokenResourceDefinitionRegistrar.KEY_CREDENTIAL);
                    break;
                }
                default: {
                    this.parseAuthTokenElement(reader, protocolAddress, operations);
                }
            }
        }

        require(reader, operation, CipherAuthTokenResourceDefinitionRegistrar.KEY_CREDENTIAL, AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
    }

    private void parseAuthTokenElement(XMLExtendedStreamReader reader, PathAddress protocolAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(protocolAddress.append(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH));
        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case SHARED_SECRET_CREDENTIAL_REFERENCE: {
                readElement(reader, operation, AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
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
        readElement(reader, operation, ProtocolChildResourceDefinitionRegistrar.PROPERTIES);
    }

    private void parseThreadPool(ThreadPoolResourceDefinitionRegistrar pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
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
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_6_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ClusteringLogger.ROOT_LOGGER.attributeIgnored(attribute.getLocalName(), reader.getLocalName());
                    break;
                case KEEPALIVE_TIME:
                    readAttribute(reader, i, operation, pool.getKeepAlive());
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    private void parseRelay(XMLExtendedStreamReader reader, PathAddress stackAddress, Map<PathAddress, ModelNode> operations, String defaultStack) throws XMLStreamException {
        PathAddress address = stackAddress.append(RelayResourceDefinitionRegistrar.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    readAttribute(reader, i, operation, RelayResourceDefinitionRegistrar.Attribute.SITE.get());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        require(reader, operation, RelayResourceDefinitionRegistrar.Attribute.SITE.get());

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SITE: {
                    this.parseRemoteSite(reader, address, operations, defaultStack);
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

    private void parseRemoteSite(XMLExtendedStreamReader reader, PathAddress relayAddress, Map<PathAddress, ModelNode> operations, String defaultStack) throws XMLStreamException {
        String site = require(reader, XMLAttribute.NAME);
        PathAddress address = relayAddress.append(RemoteSiteResourceDefinitionRegistrar.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        String cluster = null;
        String stack = defaultStack;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case STACK: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    stack = reader.getAttributeValue(i);
                    break;
                }
                case CLUSTER: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    cluster = reader.getAttributeValue(i);
                    break;
                }
                case CHANNEL: {
                    if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
                        readAttribute(reader, i, operation, RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) {
            require(reader, operation, RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION);
        } else {
            String channel = (cluster != null) ? cluster : site;
            setAttribute(reader, channel, operation, RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION);

            // We need to create a corresponding channel add operation
            PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.PATH);
            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinitionRegistrar.pathElement(channel));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            setAttribute(reader, stack, channelOperation, ChannelResourceDefinitionRegistrar.STACK);
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

    private static void require(XMLExtendedStreamReader reader, ModelNode operation, AttributeDefinition... attributes) throws XMLStreamException {
        for (AttributeDefinition attribute : attributes) {
            if (!operation.hasDefined(attribute.getName())) {
                Set<String> names = Collections.singleton(attribute.getXmlName());
                throw attribute.getParser().isParseAsElement() ? ParseUtils.missingRequiredElement(reader, names) : ParseUtils.missingRequired(reader, names);
            }
        }
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, AttributeDefinition attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getParser().parseAndSetParameter(attribute, value, operation, reader);
    }

    private static void readElement(XMLExtendedStreamReader reader, ModelNode operation, AttributeDefinition attribute) throws XMLStreamException {
        AttributeParser parser = attribute.getParser();
        if (parser.isParseAsElement()) {
            parser.parseElement(attribute, reader, operation);
        } else {
            parser.parseAndSetParameter(attribute, reader.getElementText(), operation, reader);
        }
    }
}
