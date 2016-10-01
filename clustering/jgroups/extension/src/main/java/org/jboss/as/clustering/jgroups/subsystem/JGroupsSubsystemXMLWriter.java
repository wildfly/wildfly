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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collection;
import java.util.EnumSet;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
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
    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(JGroupsSchema.CURRENT.getNamespaceUri(), false);
        ModelNode model = context.getModelNode();

        if (model.isDefined()) {
            if (model.hasDefined(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(XMLElement.CHANNELS.getLocalName());
                writeAttribute(writer, model, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL);
                for (Property property: model.get(ChannelResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(XMLElement.CHANNEL.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                    ModelNode channel = property.getValue();
                    writeAttributes(writer, channel, ChannelResourceDefinition.Attribute.class);

                    if (channel.hasDefined(ForkResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property forkProperty: channel.get(ForkResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            writer.writeStartElement(XMLElement.FORK.getLocalName());
                            writer.writeAttribute(XMLAttribute.NAME.getLocalName(), forkProperty.getName());
                            ModelNode fork = forkProperty.getValue();
                            if (fork.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                                for (Property protocol: fork.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
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
            if (model.hasDefined(StackResourceDefinition.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(XMLElement.STACKS.getLocalName());
                writeAttribute(writer, model, JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK);
                for (Property property: model.get(StackResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(XMLElement.STACK.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                    ModelNode stack = property.getValue();
                    if (stack.hasDefined(TransportResourceDefinition.WILDCARD_PATH.getKey())) {
                        writeTransport(writer, stack.get(TransportResourceDefinition.WILDCARD_PATH.getKey()).asProperty());
                    }
                    if (stack.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property protocol: stack.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            writeProtocol(writer, protocol);
                        }
                    }
                    if (stack.get(RelayResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
                        writeRelay(writer, stack.get(RelayResourceDefinition.PATH.getKeyValuePair()));
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    @SuppressWarnings("deprecation")
    private static void writeTransport(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(XMLElement.TRANSPORT.getLocalName());
        writeProtocolAttributes(writer, property);
        ModelNode transport = property.getValue();
        writeAttributes(writer, transport, TransportResourceDefinition.Attribute.class);
        writeAttributes(writer, transport, TransportResourceDefinition.ThreadingAttribute.class);
        writeElement(writer, transport, ProtocolResourceDefinition.Attribute.PROPERTIES);
        if (transport.hasDefined(ThreadPoolResourceDefinition.WILDCARD_PATH.getKey())) {
            writeThreadPoolElements(XMLElement.DEFAULT_THREAD_POOL, ThreadPoolResourceDefinition.DEFAULT, writer, transport);
            writeThreadPoolElements(XMLElement.INTERNAL_THREAD_POOL, ThreadPoolResourceDefinition.INTERNAL, writer, transport);
            writeThreadPoolElements(XMLElement.OOB_THREAD_POOL, ThreadPoolResourceDefinition.OOB, writer, transport);
            writeThreadPoolElements(XMLElement.TIMER_THREAD_POOL, ThreadPoolResourceDefinition.TIMER, writer, transport);
        }
        writer.writeEndElement();
    }

    private static void writeProtocol(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(XMLElement.PROTOCOL.getLocalName());
        writeProtocolAttributes(writer, property);
        writeElement(writer, property.getValue(), ProtocolResourceDefinition.Attribute.PROPERTIES);
        writer.writeEndElement();
    }

    private static void writeProtocolAttributes(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeAttribute(XMLAttribute.TYPE.getLocalName(), property.getName());
        writeAttributes(writer, property.getValue(), EnumSet.complementOf(EnumSet.of(ProtocolResourceDefinition.Attribute.PROPERTIES)));
    }

    private static void writeThreadPoolElements(XMLElement element, ThreadPoolResourceDefinition pool, XMLExtendedStreamWriter writer, ModelNode transport) throws XMLStreamException {
        PathElement path = pool.getPathElement();
        if (transport.get(path.getKey()).hasDefined(path.getValue())) {
            ModelNode threadPool = transport.get(path.getKeyValuePair());
            if (hasDefined(threadPool, pool.getAttributes())) {
                writer.writeStartElement(element.getLocalName());
                writeAttributes(writer, threadPool, pool.getAttributes());
                writer.writeEndElement();
            }
        }
    }

    private static void writeRelay(XMLExtendedStreamWriter writer, ModelNode relay) throws XMLStreamException {
        writer.writeStartElement(XMLElement.RELAY.getLocalName());
        writeAttributes(writer, relay, RelayResourceDefinition.Attribute.class);
        if (relay.hasDefined(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property: relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(XMLElement.REMOTE_SITE.getLocalName());
                writer.writeAttribute(XMLAttribute.NAME.getLocalName(), property.getName());
                writeAttributes(writer, property.getValue(), EnumSet.allOf(RemoteSiteResourceDefinition.Attribute.class));
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private static boolean hasDefined(ModelNode model, Collection<? extends Attribute> attributes) {
        return attributes.stream().anyMatch(attribute -> model.hasDefined(attribute.getName()));
    }

    private static <A extends Enum<A> & Attribute> void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Class<A> attributeClass) throws XMLStreamException {
        writeAttributes(writer, model, EnumSet.allOf(attributeClass));
    }

    private static void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Collection<? extends Attribute> attributes) throws XMLStreamException {
        for (Attribute attribute : attributes) {
            writeAttribute(writer, model, attribute);
        }
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getAttributeMarshaller().marshallAsAttribute(attribute.getDefinition(), model, true, writer);
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getAttributeMarshaller().marshallAsElement(attribute.getDefinition(), model, true, writer);
    }
}
