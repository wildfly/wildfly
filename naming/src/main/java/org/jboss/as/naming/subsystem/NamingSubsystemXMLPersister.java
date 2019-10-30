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

package org.jboss.as.naming.subsystem;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.REMOTE_NAMING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SERVICE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;

/**
 * @author Eduardo Martins
 */
public class NamingSubsystemXMLPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final NamingSubsystemXMLPersister INSTANCE = new NamingSubsystemXMLPersister();

    private NamingSubsystemXMLPersister() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(NamingExtension.NAMESPACE_2_0, false);

        ModelNode model = context.getModelNode();

        // bindings
        if (model.hasDefined(BINDING)) {
            writer.writeStartElement(NamingSubsystemXMLElement.BINDINGS.getLocalName());
            final ModelNode bindingModel = model.get(BINDING);
            this.writeBindings(writer, bindingModel);
            // </timer-service>
            writer.writeEndElement();
        }

        if (model.hasDefined(SERVICE)) {
            final ModelNode service = model.get(SERVICE);
            if (service.has(REMOTE_NAMING)) {
                writer.writeEmptyElement(REMOTE_NAMING);
            }
        }
        // write the subsystem end element
        writer.writeEndElement();
    }

    private void writeBindings(final XMLExtendedStreamWriter writer, final ModelNode bindingModel) throws XMLStreamException {
        for (Property binding : bindingModel.asPropertyList()) {
            final String type = binding.getValue().get(BINDING_TYPE).asString();
            if (type.equals(SIMPLE)) {
                writeSimpleBinding(binding, writer);
            } else if (type.equals(OBJECT_FACTORY)) {
                writeObjectFactoryBinding(binding, writer);
            } else if (type.equals(LOOKUP)) {
                writeLookupBinding(binding, writer);
            } else if (type.equals(EXTERNAL_CONTEXT)) {
                writeExternalContext(binding, writer);
            } else {
                throw new XMLStreamException("Unknown binding type " + type);
            }

        }
    }

    private void writeSimpleBinding(final Property binding, final XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(NamingSubsystemXMLElement.SIMPLE.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        NamingBindingResourceDefinition.VALUE.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.TYPE.marshallAsAttribute(binding.getValue(), writer);
        writer.writeEndElement();
    }

    private void writeObjectFactoryBinding(final Property binding, final XMLExtendedStreamWriter writer)
            throws XMLStreamException {

        writer.writeStartElement(NamingSubsystemXMLElement.OBJECT_FACTORY.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        NamingBindingResourceDefinition.MODULE.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.CLASS.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.ENVIRONMENT.marshallAsElement(binding.getValue(), writer);
        writer.writeEndElement();
    }


    private void writeExternalContext(final Property binding, final XMLExtendedStreamWriter writer)
            throws XMLStreamException {

        writer.writeStartElement(NamingSubsystemXMLElement.EXTERNAL_CONTEXT.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        NamingBindingResourceDefinition.MODULE.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.CLASS.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.CACHE.marshallAsAttribute(binding.getValue(), writer);
        NamingBindingResourceDefinition.ENVIRONMENT.marshallAsElement(binding.getValue(), writer);
        writer.writeEndElement();
    }

    private void writeLookupBinding(final Property binding, final XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(NamingSubsystemXMLElement.LOOKUP.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        NamingBindingResourceDefinition.LOOKUP.marshallAsAttribute(binding.getValue(), writer);
        writer.writeEndElement();
    }

}
