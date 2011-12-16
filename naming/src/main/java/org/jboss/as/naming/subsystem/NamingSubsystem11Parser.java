/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.VALUE;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Stuart Douglas
 */
public class NamingSubsystem11Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    public static final NamingSubsystem11Parser INSTANCE = new NamingSubsystem11Parser();

    private NamingSubsystem11Parser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(NamingExtension.NAMESPACE_1_1, false);

        ModelNode model = context.getModelNode();

        // bindings
        if (model.hasDefined(BINDING)) {
            writer.writeStartElement(NamingSubsystemXMLElement.BINDINGS.getLocalName());
            final ModelNode bindingModel = model.get(BINDING);
            this.writeBindings(writer, bindingModel);
            // </timer-service>
            writer.writeEndElement();
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
            } else {
                throw new XMLStreamException("Unknown binding type " + type);
            }

        }
    }

    private void writeSimpleBinding(final Property binding, final XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(NamingSubsystemXMLElement.SIMPLE.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());

        writer.writeAttribute(NamingSubsystemXMLAttribute.VALUE.getLocalName(), binding.getValue().get(VALUE).asString());
        if (binding.getValue().has(TYPE)) {
            writer.writeAttribute(NamingSubsystemXMLAttribute.TYPE.getLocalName(), binding.getValue().get(TYPE).asString());
        }
        writer.writeEndElement();
    }

    private void writeObjectFactoryBinding(final Property binding, final XMLExtendedStreamWriter writer) throws XMLStreamException {

        writer.writeStartElement(NamingSubsystemXMLElement.OBJECT_FACTORY.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.MODULE.getLocalName(), binding.getValue().get(MODULE).asString());
        writer.writeAttribute(NamingSubsystemXMLAttribute.CLASS.getLocalName(), binding.getValue().get(CLASS).asString());
        writer.writeEndElement();
    }

    private void writeLookupBinding(final Property binding, final XMLExtendedStreamWriter writer) throws XMLStreamException {

        writer.writeStartElement(NamingSubsystemXMLElement.LOOKUP.getLocalName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.NAME.getLocalName(), binding.getName());
        writer.writeAttribute(NamingSubsystemXMLAttribute.LOOKUP.getLocalName(), binding.getValue().get(LOOKUP).asString());
        writer.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {


        final ModelNode ejb3SubsystemAddOperation = new ModelNode();
        ejb3SubsystemAddOperation.get(OP).set(ADD);
        ejb3SubsystemAddOperation.get(OP_ADDR).add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);

        operations.add(ejb3SubsystemAddOperation);

        // elements
        final EnumSet<NamingSubsystemXMLElement> encountered = EnumSet.noneOf(NamingSubsystemXMLElement.class);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemNamespace.forUri(reader.getNamespaceURI())) {
                case NAMING_1_1: {
                    final NamingSubsystemXMLElement element = NamingSubsystemXMLElement.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case BINDINGS: {
                            parseBindings(reader, operations);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private void parseBindings(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // no attributes expected
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (NamingSubsystemXMLElement.forName(reader.getLocalName())) {
                case SIMPLE: {
                    this.parseSimpleBinding(reader, operations);
                    break;
                }
                case OBJECT_FACTORY: {
                    this.parseObjectFactoryBinding(reader, operations);
                    break;
                }
                case LOOKUP: {
                    this.parseLookupBinding(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private void parseSimpleBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final int attCount = reader.getAttributeCount();
        String name = null;
        String bindingValue = null;
        String type = null;
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.VALUE);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case VALUE:
                    bindingValue = NamingBindingResourceDefinition.VALUE.parse(value, reader).asString();
                    break;
                case TYPE:
                    type = NamingBindingResourceDefinition.TYPE.parse(value, reader).asString();
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, name);
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(SIMPLE);
        bindingAdd.get(VALUE).set(bindingValue);
        if (type != null) {
            bindingAdd.get(TYPE).set(type);
        }
        operations.add(bindingAdd);
    }


    private void parseObjectFactoryBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final int attCount = reader.getAttributeCount();
        String name = null;
        String module = null;
        String factory = null;
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.MODULE, NamingSubsystemXMLAttribute.CLASS);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case MODULE:
                    module = NamingBindingResourceDefinition.MODULE.parse(value, reader).asString();
                    break;
                case CLASS:
                    factory = NamingBindingResourceDefinition.CLASS.parse(value, reader).asString();
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, name);
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(OBJECT_FACTORY);
        bindingAdd.get(MODULE).set(module);
        bindingAdd.get(CLASS).set(factory);
        operations.add(bindingAdd);
    }


    private void parseLookupBinding(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final int attCount = reader.getAttributeCount();
        String name = null;
        String lookup = null;
        final EnumSet<NamingSubsystemXMLAttribute> required = EnumSet.of(NamingSubsystemXMLAttribute.NAME, NamingSubsystemXMLAttribute.LOOKUP);
        for (int i = 0; i < attCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final NamingSubsystemXMLAttribute attribute = NamingSubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = value.trim();
                    break;
                case LOOKUP:
                    lookup = NamingBindingResourceDefinition.LOOKUP.parse(value, reader).asString();
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, name);
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(LOOKUP);
        bindingAdd.get(LOOKUP).set(lookup);
        operations.add(bindingAdd);
    }


}
