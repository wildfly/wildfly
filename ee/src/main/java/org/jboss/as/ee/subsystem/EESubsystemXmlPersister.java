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

package org.jboss.as.ee.subsystem;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 */
class EESubsystemXmlPersister implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    public static final EESubsystemXmlPersister INSTANCE = new EESubsystemXmlPersister();

    private EESubsystemXmlPersister() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode eeSubSystem = context.getModelNode();
        GlobalModulesDefinition.INSTANCE.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        writeConcurrentElement(writer,eeSubSystem);
        writeDefaultBindingsElement(writer,eeSubSystem);
        writer.writeEndElement();
    }

    private void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        boolean started = false;
        if (eeSubSystem.hasDefined(EESubsystemModel.CONTEXT_SERVICE)) {
            writer.writeStartElement(Element.CONCURRENT.getLocalName());
            started = true;
            writeContextServices(writer, eeSubSystem.get(EESubsystemModel.CONTEXT_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_THREAD_FACTORY)) {
            if(!started) {
                writer.writeStartElement(Element.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedThreadFactories(writer, eeSubSystem.get(EESubsystemModel.MANAGED_THREAD_FACTORY));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_EXECUTOR_SERVICE)) {
            if(!started) {
                writer.writeStartElement(Element.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_EXECUTOR_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE)) {
            if(!started) {
                writer.writeStartElement(Element.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedScheduledExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
        if(started) {
            writer.writeEndElement();
        }
    }

    private void writeContextServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.CONTEXT_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(Element.CONTEXT_SERVICE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ContextServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeManagedThreadFactories(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.MANAGED_THREAD_FACTORIES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(Element.MANAGED_THREAD_FACTORY.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedThreadFactoryResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeManagedExecutorServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.MANAGED_EXECUTOR_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(Element.MANAGED_EXECUTOR_SERVICE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedExecutorServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeManagedScheduledExecutorServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.MANAGED_SCHEDULED_EXECUTOR_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(Element.MANAGED_SCHEDULED_EXECUTOR_SERVICE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedScheduledExecutorServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeDefaultBindingsElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        if (eeSubSystem.hasDefined(EESubsystemModel.SERVICE) && eeSubSystem.get(EESubsystemModel.SERVICE).hasDefined(EESubsystemModel.DEFAULT_BINDINGS)) {
            ModelNode defaultBindingsNode = eeSubSystem.get(EESubsystemModel.SERVICE, EESubsystemModel.DEFAULT_BINDINGS);
            writer.writeStartElement(Element.DEFAULT_BINDINGS.getLocalName());
            for(SimpleAttributeDefinition ad : DefaultBindingsResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(defaultBindingsNode,writer);
            }
            writer.writeEndElement();
        }
    }

}
