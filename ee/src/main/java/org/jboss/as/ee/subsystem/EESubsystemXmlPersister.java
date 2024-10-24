/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
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
        writeGlobalDirectoryElement(writer, eeSubSystem);
        EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.marshallAsElement(eeSubSystem, writer);
        ConcurrencyImplementation.INSTANCE.writeConcurrentElement(writer,eeSubSystem);
        writeDefaultBindingsElement(writer,eeSubSystem);
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

    private void writeGlobalDirectoryElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        if (eeSubSystem.hasDefined(EESubsystemModel.GLOBAL_DIRECTORY)) {
            writer.writeStartElement(Element.GLOBAL_DIRECTORIES.getLocalName());
            writeDirectoryElement(writer, eeSubSystem.get(EESubsystemModel.GLOBAL_DIRECTORY));
            writer.writeEndElement();
        }
    }

    private void writeDirectoryElement(final XMLExtendedStreamWriter writer, final ModelNode subModel) throws XMLStreamException {
        for (Property property : subModel.asPropertyList()) {
            writer.writeStartElement(Element.DIRECTORY.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            for (SimpleAttributeDefinition ad : GlobalDirectoryResourceDefinition.ATTRIBUTES) {
                ad.marshallAsAttribute(property.getValue(), writer);
            }
            writer.writeEndElement();
        }
    }
}
