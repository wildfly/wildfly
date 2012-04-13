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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.remoting.Attribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_DISTINCT_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ENABLE_STATISTICS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;


/**
 */
public class EJB3Subsystem13Parser extends EJB3Subsystem12Parser {

    public static final EJB3Subsystem13Parser INSTANCE = new EJB3Subsystem13Parser();

    protected EJB3Subsystem13Parser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeElements(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        super.writeElements(writer, context);

        ModelNode model = context.getModelNode();

        // statistics element
        if (model.hasDefined(ENABLE_STATISTICS)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.STATISTICS.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.ENABLED.getLocalName(), model.get(EJB3SubsystemModel.ENABLE_STATISTICS).asString());
            writer.writeEndElement();
        }

        // default-distinct-name element
        if (model.hasDefined(DEFAULT_DISTINCT_NAME)) {
            writer.writeStartElement(EJB3SubsystemXMLElement.DEFAULT_DISTINCT_NAME.getLocalName());
            writer.writeAttribute(EJB3SubsystemXMLAttribute.VALUE.getLocalName(), model.get(EJB3SubsystemModel.DEFAULT_DISTINCT_NAME).asString());
            writer.writeEndElement();
        }
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case DEFAULT_DISTINCT_NAME: {
                parseDefaultDistinctName(reader, ejb3SubsystemAddOperation);
                break;
            }
            case STATISTICS: {
                parseStatistics(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_1_3;
    }

    private void parseStatistics(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.ENABLED);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED:
                    EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.ENABLED);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    private void parseDefaultDistinctName(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.DEFAULT_DISTINCT_NAME.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    @Override
    protected void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String connectorName = null;
        String threadPoolName = null;
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTOR_REF, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CONNECTOR_REF:
                    connectorName = value;
                    break;
                case THREAD_POOL_NAME:
                    threadPoolName = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        operations.add(EJB3RemoteServiceAdd.create(connectorName, threadPoolName));

        // set the address for this operation
        final ModelNode ejb3RemoteServiceAddress = new ModelNode();
        ejb3RemoteServiceAddress.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        ejb3RemoteServiceAddress.add(SERVICE, REMOTE);

        final Set<EJB3SubsystemXMLElement> parsedElements = new HashSet<EJB3SubsystemXMLElement>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CHANNEL_CREATION_OPTIONS: {
                    if (parsedElements.contains(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS)) {
                        throw unexpectedElement(reader);
                    }
                    parsedElements.add(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS);
                    this.parseChannelCreationOptions(reader, ejb3RemoteServiceAddress, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private void parseChannelCreationOptions(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operations) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case OPTION: {
                    this.parseChannelCreationOption(reader, address, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseChannelCreationOption(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operations) throws XMLStreamException {
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.TYPE);
        final int count = reader.getAttributeCount();
        String optionName = null;
        String optionType = null;
        String optionValue = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    optionName = attributeValue;
                    break;
                case TYPE:
                    optionType = attributeValue;
                    break;
                case VALUE:
                    optionValue = attributeValue;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // this element just supports attributes so we expect no more content
        requireNoContent(reader);

        final ModelNode channelOptionAddOperation = new ModelNode();
        channelOptionAddOperation.get(OP).set(ADD);
        channelOptionAddOperation.get(OP_ADDR).set(address).add(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS, optionName);
        channelOptionAddOperation.get(VALUE).set(optionValue);
        channelOptionAddOperation.get(EJB3SubsystemModel.TYPE).set(optionType);

        operations.add(channelOptionAddOperation);
    }

    @Override
    protected void writeRemote(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        super.writeRemote(writer, model);

        // write out any channel creation options
        if (model.hasDefined(CHANNEL_CREATION_OPTIONS)) {
            writeChannelCreationOptions(writer, model.get(CHANNEL_CREATION_OPTIONS));
        }
    }

    private void writeChannelCreationOptions(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS.getLocalName());
        for (final Property optionPropertyModelNode : node.asPropertyList()) {
            writer.writeStartElement(EJB3SubsystemXMLElement.OPTION.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), optionPropertyModelNode.getName());
            final ModelNode propertyValueModelNode = optionPropertyModelNode.getValue();
            ChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.marshallAsAttribute(propertyValueModelNode, writer);
            ChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.marshallAsAttribute(propertyValueModelNode, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
