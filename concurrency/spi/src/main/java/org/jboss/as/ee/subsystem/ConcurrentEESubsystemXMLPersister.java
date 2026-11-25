/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * The persister for the 'concurrent' XML element EE Subsystem configuration.
 * @author emartins
 */
public class ConcurrentEESubsystemXMLPersister {
    private ConcurrentEESubsystemXMLPersister() {
    }
    public static void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        boolean started = false;
        if (eeSubSystem.hasDefined(EESubsystemModel.CONTEXT_SERVICE)) {
            writer.writeStartElement(ConcurrentElement.CONCURRENT.getLocalName());
            started = true;
            writeContextServices(writer, eeSubSystem.get(EESubsystemModel.CONTEXT_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_THREAD_FACTORY)) {
            if(!started) {
                writer.writeStartElement(ConcurrentElement.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedThreadFactories(writer, eeSubSystem.get(EESubsystemModel.MANAGED_THREAD_FACTORY));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_EXECUTOR_SERVICE)) {
            if(!started) {
                writer.writeStartElement(ConcurrentElement.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_EXECUTOR_SERVICE));
        }
        if (eeSubSystem.hasDefined(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE)) {
            if(!started) {
                writer.writeStartElement(ConcurrentElement.CONCURRENT.getLocalName());
                started = true;
            }
            writeManagedScheduledExecutorServices(writer, eeSubSystem.get(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        }
        if(started) {
            writer.writeEndElement();
        }
    }

    private static void writeContextServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(ConcurrentElement.CONTEXT_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(ConcurrentElement.CONTEXT_SERVICE.getLocalName());
            writer.writeAttribute(ConcurrentAttribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ContextServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeManagedThreadFactories(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(ConcurrentElement.MANAGED_THREAD_FACTORIES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(ConcurrentElement.MANAGED_THREAD_FACTORY.getLocalName());
            writer.writeAttribute(ConcurrentAttribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedThreadFactoryResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeManagedExecutorServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(ConcurrentElement.MANAGED_EXECUTOR_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(ConcurrentElement.MANAGED_EXECUTOR_SERVICE.getLocalName());
            writer.writeAttribute(ConcurrentAttribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedExecutorServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeManagedScheduledExecutorServices(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        writer.writeStartElement(ConcurrentElement.MANAGED_SCHEDULED_EXECUTOR_SERVICES.getLocalName());
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(ConcurrentElement.MANAGED_SCHEDULED_EXECUTOR_SERVICE.getLocalName());
            writer.writeAttribute(ConcurrentAttribute.NAME.getLocalName(), property.getName());
            for(SimpleAttributeDefinition ad : ManagedScheduledExecutorServiceResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
