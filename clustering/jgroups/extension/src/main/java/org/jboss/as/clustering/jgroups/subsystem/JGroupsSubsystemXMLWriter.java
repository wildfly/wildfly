/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Tristan Tarrant
 */
public class JGroupsSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(JGroupsSubsystemSchema.CURRENT.getNamespace().getUri(), false);
        ModelNode model = context.getModelNode();

        if (model.isDefined()) {
            if (model.hasDefined(ChannelResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(XMLElement.CHANNELS.getLocalName());
                writeAttributes(writer, model, JGroupsSubsystemResourceDefinitionRegistrar.attributes());
                for (Property property: model.get(ChannelResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(XMLElement.CHANNEL.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                    ModelNode channel = property.getValue();
                    writeAttributes(writer, channel, ChannelResourceDefinitionRegistrar.attributes());

                    if (channel.hasDefined(ForkResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                        for (Property forkProperty: channel.get(ForkResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                            writer.writeStartElement(XMLElement.FORK.getLocalName());
                            writer.writeAttribute(XMLAttribute.NAME.getLocalName(), forkProperty.getName());
                            ModelNode fork = forkProperty.getValue();
                            if (fork.hasDefined(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                                for (Property protocol: fork.get(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                                    writeProtocol(writer, protocol);
                                }
                            }
                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            if (model.hasDefined(StackResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(XMLElement.STACKS.getLocalName());
                for (Property property: model.get(StackResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(XMLElement.STACK.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                    ModelNode stack = property.getValue();
                    writeAttributes(writer, stack, StackResourceDefinitionRegistrar.attributes());
                    writeTransport(writer, stack.get(TransportResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asProperty());
                    if (stack.hasDefined(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
                        for (Property protocol: stack.get(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                            writeProtocol(writer, protocol);
                        }
                    }
                    if (stack.hasDefined(RelayResourceDefinitionRegistrar.PATH.getKeyValuePair())) {
                        writeRelay(writer, stack.get(RelayResourceDefinitionRegistrar.PATH.getKeyValuePair()));
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private static void writeTransport(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(XMLElement.TRANSPORT.getLocalName());
        ModelNode transport = property.getValue();
        writer.writeAttribute(XMLAttribute.TYPE.getLocalName(), property.getName());
        if (containsName(TransportResourceDefinitionRegistrar.SocketTransport.class, property.getName())) {
            writeAttributes(writer, property.getValue(), SocketTransportResourceDefinitionRegistrar.attributes());
        } else {
            writeAttributes(writer, transport, TransportResourceDefinitionRegistrar.attributes());
        }
        writeThreadPoolElements(XMLElement.DEFAULT_THREAD_POOL, ThreadPoolResourceDefinitionRegistrar.DEFAULT, writer, transport);
        writer.writeEndElement();
    }

    private static void writeProtocol(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(XMLElement.forProtocolName(property).getLocalName());
        writer.writeAttribute(XMLAttribute.TYPE.getLocalName(), property.getName());
        String protocolName = property.getName();
        if (containsName(ProtocolResourceDefinitionRegistrar.MulticastProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), MulticastProtocolResourceDefinitionRegistrar.attributes());
        } else if (containsName(ProtocolResourceDefinitionRegistrar.SocketProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), SocketProtocolResourceDefinitionRegistrar.attributes());
        } else if (containsName(ProtocolResourceDefinitionRegistrar.JdbcProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), JDBCProtocolResourceDefinitionRegistrar.attributes());
        } else if (containsName(ProtocolResourceDefinitionRegistrar.EncryptProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), EncryptProtocolResourceDefinitionRegistrar.attributes());
        } else if (containsName(ProtocolResourceDefinitionRegistrar.InitialHostsProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), SocketDiscoveryProtocolResourceDefinitionRegistrar.attributes());
        } else if (containsName(ProtocolResourceDefinitionRegistrar.AuthProtocol.class, protocolName)) {
            writeAttributes(writer, property.getValue(), AuthProtocolResourceDefinitionRegistrar.attributes());
            writeAuthToken(writer, property.getValue().get(AuthTokenResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asProperty());
        } else {
            writeAttributes(writer, property.getValue(), ProtocolResourceDefinitionRegistrar.attributes());
        }
        writer.writeEndElement();
    }

    private static <E extends Enum<E>> boolean containsName(Class<E> enumClass, String name) {
        for (E protocol : EnumSet.allOf(enumClass)) {
            if (name.equals(protocol.name())) {
                return true;
            }
        }
        return false;
    }

    private static void writeAuthToken(XMLExtendedStreamWriter writer, Property token) throws XMLStreamException {
        writer.writeStartElement(XMLElement.forAuthTokenName(token.getName()).getLocalName());

        if (PlainAuthTokenResourceDefinitionRegistrar.PATH.getValue().equals(token.getName())) {
            writeAttributes(writer, token.getValue(), PlainAuthTokenResourceDefinitionRegistrar.attributes());
        } else if (DigestAuthTokenResourceDefinitionRegistrar.PATH.getValue().equals(token.getName())) {
            writeAttributes(writer, token.getValue(), DigestAuthTokenResourceDefinitionRegistrar.attributes());
        } else if (CipherAuthTokenResourceDefinitionRegistrar.PATH.getValue().equals(token.getName())) {
            writeAttributes(writer, token.getValue(), CipherAuthTokenResourceDefinitionRegistrar.attributes());
        }

        writer.writeEndElement();
    }

    private static void writeThreadPoolElements(XMLElement element, ThreadPoolResourceDefinitionRegistrar pool, XMLExtendedStreamWriter writer, ModelNode transport) throws XMLStreamException {
        PathElement path = pool.getPathElement();
        if (transport.get(path.getKey()).hasDefined(path.getValue())) {
            ModelNode threadPool = transport.get(path.getKeyValuePair());
            if (pool.getAttributes().map(AttributeDefinition::getName).anyMatch(threadPool::hasDefined)) {
                writer.writeStartElement(element.getLocalName());
                writeAttributes(writer, threadPool, pool.getAttributes());
                writer.writeEndElement();
            }
        }
    }

    private static void writeRelay(XMLExtendedStreamWriter writer, ModelNode relay) throws XMLStreamException {
        writer.writeStartElement(XMLElement.RELAY.getLocalName());
        writeAttributes(writer, relay, RelayResourceDefinitionRegistrar.attributes());
        if (relay.hasDefined(RemoteSiteResourceDefinitionRegistrar.WILDCARD_PATH.getKey())) {
            for (Property property: relay.get(RemoteSiteResourceDefinitionRegistrar.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(XMLElement.REMOTE_SITE.getLocalName());
                writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                writeAttributes(writer, property.getValue(), RemoteSiteResourceDefinitionRegistrar.attributes());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private static void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Stream<AttributeDefinition> stream) throws XMLStreamException {
        // Write attributes before elements
        List<AttributeDefinition> elementAttributes = new LinkedList<>();
        Iterator<AttributeDefinition> attributes = stream.iterator();
        while (attributes.hasNext()) {
            AttributeDefinition attribute = attributes.next();
            if (attribute.getMarshaller().isMarshallableAsElement()) {
                elementAttributes.add(attribute);
            } else {
                writeAttribute(writer, model, attribute);
            }
        }
        for (AttributeDefinition attribute : elementAttributes) {
            writeElement(writer, model, attribute);
        }
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getMarshaller().marshallAsAttribute(attribute, model, true, writer);
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getMarshaller().marshallAsElement(attribute, model, true, writer);
    }
}
