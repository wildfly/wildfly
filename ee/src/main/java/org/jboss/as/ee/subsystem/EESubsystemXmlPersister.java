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
        context.startSubsystemElement(Namespace.EE_2_0.getUriString(), false);

        ModelNode eeSubSystem = context.getModelNode();
        GlobalModulesDefinition.INSTANCE.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.READ_ONLY_NAMING_CONTEXTS.marshallAsElement(eeSubSystem, writer);
        writeConcurrentElement(writer,eeSubSystem);
        writer.writeEndElement();
    }

    private void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        // default resources
        if (eeSubSystem.hasDefined(EESubsystemModel.SERVICE) && eeSubSystem.get(EESubsystemModel.SERVICE).hasDefined(EESubsystemModel.DEFAULT_CONTEXT_SERVICE)) {
            writer.writeStartElement(Element.CONCURRENT.getLocalName());
            writeDefaultContextService(writer, eeSubSystem.get(EESubsystemModel.SERVICE, EESubsystemModel.DEFAULT_CONTEXT_SERVICE));
        } else {
            // EE Concurrent off
            return;
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.SERVICE) && eeSubSystem.get(EESubsystemModel.SERVICE).hasDefined(EESubsystemModel.DEFAULT_MANAGED_THREAD_FACTORY)) {
            writeDefaultManagedThreadFactory(writer, eeSubSystem.get(EESubsystemModel.SERVICE,EESubsystemModel.DEFAULT_MANAGED_THREAD_FACTORY));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.SERVICE) && eeSubSystem.get(EESubsystemModel.SERVICE).hasDefined(EESubsystemModel.DEFAULT_MANAGED_EXECUTOR_SERVICE)) {
            writeDefaultManagedExecutorService(writer, eeSubSystem.get(EESubsystemModel.SERVICE, EESubsystemModel.DEFAULT_MANAGED_EXECUTOR_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.SERVICE) && eeSubSystem.get(EESubsystemModel.SERVICE).hasDefined(EESubsystemModel.DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE)) {
            writeDefaultManagedScheduledExecutorService(writer, eeSubSystem.get(EESubsystemModel.SERVICE, EESubsystemModel.DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
        // user resources
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_THREAD_FACTORY)) {
            writeManagedThreadFactories(writer, eeSubSystem.get(EESubsystemModel.MANAGED_THREAD_FACTORY));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_EXECUTOR_SERVICE)) {
            writeManagedExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_EXECUTOR_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE)) {
            writeManagedScheduledExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
        writer.writeEndElement();
    }

    private void writeDefaultContextService(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.DEFAULT_CONTEXT_SERVICE.getLocalName());
        for(SimpleAttributeDefinition ad : DefaultContextServiceResourceDefinition.ATTRIBUTES) {
            ad.marshallAsAttribute(subModel, writer);
        }
        writer.writeEndElement();
    }

    private void writeDefaultManagedThreadFactory(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.DEFAULT_MANAGED_THREAD_FACTORY.getLocalName());
        for(SimpleAttributeDefinition ad : DefaultManagedThreadFactoryResourceDefinition.ATTRIBUTES) {
            ad.marshallAsAttribute(subModel, writer);
        }
        writer.writeEndElement();
    }

    private void writeDefaultManagedExecutorService(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.DEFAULT_MANAGED_EXECUTOR_SERVICE.getLocalName());
        for(SimpleAttributeDefinition ad : DefaultManagedExecutorServiceResourceDefinition.ATTRIBUTES) {
            ad.marshallAsAttribute(subModel, writer);
        }
        writer.writeEndElement();
    }

    private void writeDefaultManagedScheduledExecutorService(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(Element.DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE.getLocalName());
        for(SimpleAttributeDefinition ad : DefaultManagedScheduledExecutorServiceResourceDefinition.ATTRIBUTES) {
            ad.marshallAsAttribute(subModel, writer);
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

}
