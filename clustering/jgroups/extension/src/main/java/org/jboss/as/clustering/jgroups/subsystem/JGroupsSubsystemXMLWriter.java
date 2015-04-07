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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
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
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(JGroupsSchema.CURRENT.getNamespaceUri(), false);
        ModelNode model = context.getModelNode();

        if (model.isDefined()) {
            if (model.hasDefined(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(Element.CHANNELS.getLocalName());
                JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.marshallAsAttribute(model, writer);
                for (Property property: model.get(ChannelResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(Element.CHANNEL.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    ModelNode channel = property.getValue();
                    writeAttribute(writer, channel, ChannelResourceDefinition.STACK);
                    writeAttribute(writer, channel, ChannelResourceDefinition.MODULE);

                    if (channel.hasDefined(ForkResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property forkProperty: channel.get(ForkResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            writer.writeStartElement(Element.FORK.getLocalName());
                            writer.writeAttribute(Attribute.NAME.getLocalName(), forkProperty.getName());
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
                writer.writeStartElement(Element.STACKS.getLocalName());
                writeAttribute(writer, model, JGroupsSubsystemResourceDefinition.DEFAULT_STACK);
                for (Property property: model.get(StackResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(Element.STACK.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
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

    private static void writeTransport(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(Element.TRANSPORT.getLocalName());
        writeProtocolAttributes(writer, property);
        ModelNode transport = property.getValue();
        writeAttribute(writer, transport, TransportResourceDefinition.SHARED);
        writeAttribute(writer, transport, TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING);
        writeAttribute(writer, transport, TransportResourceDefinition.DEFAULT_EXECUTOR);
        writeAttribute(writer, transport, TransportResourceDefinition.OOB_EXECUTOR);
        writeAttribute(writer, transport, TransportResourceDefinition.TIMER_EXECUTOR);
        writeAttribute(writer, transport, TransportResourceDefinition.THREAD_FACTORY);
        writeAttribute(writer, transport, TransportResourceDefinition.MACHINE);
        writeAttribute(writer, transport, TransportResourceDefinition.RACK);
        writeAttribute(writer, transport, TransportResourceDefinition.SITE);
        writeElement(writer, transport, ProtocolResourceDefinition.PROPERTIES);
        if (transport.hasDefined(ThreadPoolResourceDefinition.WILDCARD_PATH.getKey())) {
            writeThreadPoolElements(Element.DEFAULT_THREAD_POOL, ThreadPoolResourceDefinition.DEFAULT, writer, transport);
            writeThreadPoolElements(Element.INTERNAL_THREAD_POOL, ThreadPoolResourceDefinition.INTERNAL, writer, transport);
            writeThreadPoolElements(Element.OOB_THREAD_POOL, ThreadPoolResourceDefinition.OOB, writer, transport);
            writeThreadPoolElements(Element.TIMER_THREAD_POOL, ThreadPoolResourceDefinition.TIMER, writer, transport);
        }
        writer.writeEndElement();
    }

    private static void writeProtocol(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(Element.PROTOCOL.getLocalName());
        writeProtocolAttributes(writer, property);
        writeElement(writer, property.getValue(), ProtocolResourceDefinition.PROPERTIES);
        writer.writeEndElement();
    }

    private static void writeProtocolAttributes(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeAttribute(Attribute.TYPE.getLocalName(), property.getName());
        ModelNode protocol = property.getValue();
        writeAttribute(writer, protocol, ProtocolResourceDefinition.SOCKET_BINDING);
        writeAttribute(writer, protocol, ProtocolResourceDefinition.MODULE);
    }

    private static void writeThreadPoolElements(Element element, ThreadPoolResourceDefinition pool, XMLExtendedStreamWriter writer, ModelNode transport) throws XMLStreamException {
        if (transport.get(pool.getPathElement().getKey()).hasDefined(pool.getPathElement().getValue())) {
            ModelNode threadPool = transport.get(pool.getPathElement().getKeyValuePair());
            writer.writeStartElement(element.getLocalName());
            writeAttribute(writer, threadPool, pool.getMinThreads());
            writeAttribute(writer, threadPool, pool.getMaxThreads());
            writeAttribute(writer, threadPool, pool.getQueueLength());
            writeAttribute(writer, threadPool, pool.getKeepaliveTime());
            writer.writeEndElement();
        }
    }

    private static void writeRelay(XMLExtendedStreamWriter writer, ModelNode relay) throws XMLStreamException {
        writer.writeStartElement(Element.RELAY.getLocalName());
        RelayResourceDefinition.SITE.marshallAsAttribute(relay, writer);
        if (relay.hasDefined(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property: relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(Element.REMOTE_SITE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                ModelNode remoteSite = property.getValue();
                writeAttribute(writer, remoteSite, RemoteSiteResourceDefinition.CHANNEL);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getAttributeMarshaller().marshallAsAttribute(attribute, model, true, writer);
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getAttributeMarshaller().marshallAsElement(attribute, model, true, writer);
    }
}
