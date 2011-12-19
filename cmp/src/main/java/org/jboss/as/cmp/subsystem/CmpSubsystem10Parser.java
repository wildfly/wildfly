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

package org.jboss.as.cmp.subsystem;

import java.util.Collections;
import java.util.List;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import javax.xml.stream.XMLStreamException;
import static org.jboss.as.cmp.subsystem.CmpConstants.BLOCK_SIZE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE_DDL;
import static org.jboss.as.cmp.subsystem.CmpConstants.DATA_SOURCE;
import static org.jboss.as.cmp.subsystem.CmpConstants.DROP_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpConstants.ID_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SELECT_HI_DDL;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_NAME;
import static org.jboss.as.cmp.subsystem.CmpConstants.TABLE_NAME;
import static org.jboss.as.cmp.subsystem.CmpConstants.UUID_KEY_GENERATOR;
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
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class CmpSubsystem10Parser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    static CmpSubsystem10Parser INSTANCE = new CmpSubsystem10Parser();

    private CmpSubsystem10Parser() {
    }

    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, CmpExtension.SUBSYSTEM_NAME);
        address.protect();

        requireNoAttributes(reader);
        final ModelNode cmpSubsystem = new ModelNode();
        cmpSubsystem.get(OP).set(ADD);
        cmpSubsystem.get(OP_ADDR).set(address);
        operations.add(cmpSubsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case KEY_GENERATORS: {
                    this.parseKeyGenerators(reader, operations, address);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseKeyGenerators(final XMLExtendedStreamReader reader, final List<ModelNode> operations, final ModelNode parentAddress) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case UUID: {
                    operations.add(parseUuid(reader, parentAddress));
                    break;
                }
                case HILO: {
                    operations.add(parseHilo(reader, parentAddress));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseUuid(final XMLExtendedStreamReader reader, final ModelNode parentAddress) throws XMLStreamException {
        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        final ModelNode address = parentAddress.clone();
        address.add(UUID_KEY_GENERATOR, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        requireNoContent(reader);
        return op;
    }

    private ModelNode parseHilo(final XMLExtendedStreamReader reader, final ModelNode parentAddress) throws XMLStreamException {
        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        final ModelNode address = parentAddress.clone();
        address.add(HILO_KEY_GENERATOR, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String value = reader.getElementText();
            switch (Element.forName(reader.getLocalName())) {
                case BLOCK_SIZE: {
                    op.get(CmpConstants.BLOCK_SIZE).set(Long.parseLong(value));
                    break;
                }
                case CREATE_TABLE: {
                    op.get(CmpConstants.CREATE_TABLE).set(Boolean.parseBoolean(value));
                    break;
                }
                case CREATE_TABLE_DDL: {
                    op.get(CmpConstants.CREATE_TABLE_DDL).set(value);
                    break;
                }
                case DATA_SOURCE: {
                    op.get(CmpConstants.DATA_SOURCE).set(value);
                    break;
                }
                case DROP_TABLE: {
                    op.get(DROP_TABLE).set(Boolean.parseBoolean(value));
                    break;
                }
                case ID_COLUMN: {
                    op.get(ID_COLUMN).set(value);
                    break;
                }
                case SELECT_HI_DDL: {
                    op.get(CmpConstants.SELECT_HI_DDL).set(value);
                    break;
                }
                case SEQUENCE_COLUMN: {
                    op.get(CmpConstants.SEQUENCE_COLUMN).set(value);
                    break;
                }
                case SEQUENCE_NAME: {
                    op.get(CmpConstants.SEQUENCE_NAME).set(value);
                    break;
                }
                case TABLE_NAME: {
                    op.get(CmpConstants.TABLE_NAME).set(value);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return op;
    }

    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CMP_1_0.getUriString(), false);

        final ModelNode model = context.getModelNode();
        if (model.hasDefined(UUID_KEY_GENERATOR) || model.hasDefined(HILO_KEY_GENERATOR)) {
            writer.writeStartElement(Element.KEY_GENERATORS.getLocalName());

            if (model.hasDefined(UUID_KEY_GENERATOR)) {
                for (Property keyGen : model.get(UUID_KEY_GENERATOR).asPropertyList()) {
                    final String name = keyGen.getName();
                    writeUuid(writer, name);
                }
            }
            if (model.hasDefined(HILO_KEY_GENERATOR)) {
                for (Property keyGen : model.get(HILO_KEY_GENERATOR).asPropertyList()) {
                    final String name = keyGen.getName();
                    final ModelNode keyGenModel = keyGen.getValue();
                    writeHilo(writer, name, keyGenModel);
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeHilo(final XMLExtendedStreamWriter writer, final String name, final ModelNode model) throws XMLStreamException {
        writer.writeStartElement(Element.HILO.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);

        if (model.hasDefined(BLOCK_SIZE)) {
            writer.writeStartElement(Element.BLOCK_SIZE.getLocalName());
            writer.writeCharacters(model.get(BLOCK_SIZE).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(CREATE_TABLE)) {
            writer.writeStartElement(Element.CREATE_TABLE.getLocalName());
            writer.writeCharacters(model.get(CREATE_TABLE).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(CREATE_TABLE_DDL)) {
            writer.writeStartElement(Element.CREATE_TABLE_DDL.getLocalName());
            writer.writeCharacters(model.get(CREATE_TABLE_DDL).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(DATA_SOURCE)) {
            writer.writeStartElement(Element.DATA_SOURCE.getLocalName());
            writer.writeCharacters(model.get(DATA_SOURCE).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(DROP_TABLE)) {
            writer.writeStartElement(Element.DROP_TABLE.getLocalName());
            writer.writeCharacters(model.get(DROP_TABLE).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(ID_COLUMN)) {
            writer.writeStartElement(Element.ID_COLUMN.getLocalName());
            writer.writeCharacters(model.get(ID_COLUMN).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(SELECT_HI_DDL)) {
            writer.writeStartElement(Element.SELECT_HI_DDL.getLocalName());
            writer.writeCharacters(model.get(SELECT_HI_DDL).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(SEQUENCE_COLUMN)) {
            writer.writeStartElement(Element.SEQUENCE_COLUMN.getLocalName());
            writer.writeCharacters(model.get(SEQUENCE_COLUMN).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(SEQUENCE_NAME)) {
            writer.writeStartElement(Element.SEQUENCE_NAME.getLocalName());
            writer.writeCharacters(model.get(SEQUENCE_NAME).asString());
            writer.writeEndElement();
        }
        if (model.hasDefined(TABLE_NAME)) {
            writer.writeStartElement(Element.TABLE_NAME.getLocalName());
            writer.writeCharacters(model.get(TABLE_NAME).asString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeUuid(final XMLExtendedStreamWriter writer, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.UUID.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        writer.writeEndElement();
    }
}
