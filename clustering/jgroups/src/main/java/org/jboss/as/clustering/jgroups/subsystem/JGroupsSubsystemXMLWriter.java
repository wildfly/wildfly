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
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            this.writeOptional(writer, Attribute.DEFAULT_STACK, model, ModelKeys.DEFAULT_STACK);
            for (Property property: model.get(ModelKeys.STACK).asPropertyList()) {
                writer.writeStartElement(Element.STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                ModelNode stack = property.getValue();
                this.writeProtocol(writer, stack.get(ModelKeys.TRANSPORT), Element.TRANSPORT);
                if (stack.hasDefined(ModelKeys.PROTOCOLS)) {
                    for (ModelNode protocol: stack.get(ModelKeys.PROTOCOLS).asList()) {
                        this.writeProtocol(writer, protocol, Element.PROTOCOL);
                    }
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeProtocol(XMLExtendedStreamWriter writer, ModelNode protocol, Element element) throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
        this.writeRequired(writer, Attribute.TYPE, protocol, ModelKeys.TYPE);
        this.writeOptional(writer, Attribute.SHARED, protocol, ModelKeys.SHARED);
        this.writeOptional(writer, Attribute.SOCKET_BINDING, protocol, ModelKeys.SOCKET_BINDING);
        this.writeOptional(writer, Attribute.DIAGNOSTICS_SOCKET_BINDING, protocol, ModelKeys.DIAGNOSTICS_SOCKET_BINDING);
        this.writeOptional(writer, Attribute.DEFAULT_EXECUTOR, protocol, ModelKeys.DEFAULT_EXECUTOR);
        this.writeOptional(writer, Attribute.OOB_EXECUTOR, protocol, ModelKeys.OOB_EXECUTOR);
        this.writeOptional(writer, Attribute.TIMER_EXECUTOR, protocol, ModelKeys.TIMER_EXECUTOR);
        this.writeOptional(writer, Attribute.THREAD_FACTORY, protocol, ModelKeys.THREAD_FACTORY);
        this.writeOptional(writer, Attribute.MACHINE, protocol, ModelKeys.MACHINE);
        this.writeOptional(writer, Attribute.RACK, protocol, ModelKeys.RACK);
        this.writeOptional(writer, Attribute.SITE, protocol, ModelKeys.SITE);
        if (protocol.has(ModelKeys.PROPERTIES)) {
            for (Property property: protocol.get(ModelKeys.PROPERTIES).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeRequired(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), model.require(key).asString());
    }

    private void writeOptional(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        if (model.hasDefined(key)) {
            writer.writeAttribute(attribute.getLocalName(), model.get(key).asString());
        }
    }
}
