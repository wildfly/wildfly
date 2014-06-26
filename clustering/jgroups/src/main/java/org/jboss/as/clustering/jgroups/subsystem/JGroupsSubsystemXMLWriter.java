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
            JGroupsSubsystemResourceDefinition.DEFAULT_STACK.marshallAsAttribute(model, writer);
            if (model.hasDefined(StackResourceDefinition.WILDCARD_PATH.getKey())) {
                // each property represents a stack
                for (Property property: model.get(StackResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(Element.STACK.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    ModelNode stack = property.getValue();
                    if (stack.get(TransportResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
                        ModelNode transport = stack.get(TransportResourceDefinition.PATH.getKeyValuePair());
                        writer.writeStartElement(Element.TRANSPORT.getLocalName());
                        TransportResourceDefinition.TYPE.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.SOCKET_BINDING.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.SHARED.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.DEFAULT_EXECUTOR.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.OOB_EXECUTOR.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.TIMER_EXECUTOR.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.THREAD_FACTORY.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.MACHINE.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.RACK.marshallAsAttribute(transport, writer);
                        TransportResourceDefinition.SITE.marshallAsAttribute(transport, writer);
                        writeProtocolProperties(writer, transport);
                        writer.writeEndElement();
                    }
                    // write the protocols in their correct order
                    if (stack.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property protocolProperty: StackAddHandler.getOrderedProtocolPropertyList(stack)) {
                            ModelNode protocol = protocolProperty.getValue();
                            writer.writeStartElement(Element.PROTOCOL.getLocalName());
                            ProtocolResourceDefinition.TYPE.marshallAsAttribute(protocol, writer);
                            ProtocolResourceDefinition.SOCKET_BINDING.marshallAsAttribute(protocol, writer);
                            writeProtocolProperties(writer, protocol);
                            writer.writeEndElement();
                        }
                    }
                    if (stack.get(RelayResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
                        ModelNode relay = stack.get(RelayResourceDefinition.PATH.getKeyValuePair());
                        writeRelay(writer, relay);
                    }
                    writer.writeEndElement();
                }
            }
        }
        writer.writeEndElement();
    }

    private static void writeProtocolProperties(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException {
        // the format of the property elements
        //  "property" => {
        //       "relative-to" => {"value" => "fred"},
        //   }
        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property: protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                Property complexValue = property.getValue().asProperty();
                writer.writeCharacters(complexValue.getValue().asString());
                writer.writeEndElement();
            }
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
                RemoteSiteResourceDefinition.STACK.marshallAsAttribute(remoteSite, writer);
                RemoteSiteResourceDefinition.CLUSTER.marshallAsAttribute(remoteSite, writer);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }
}
