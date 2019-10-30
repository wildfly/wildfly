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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;


/**
 */
public class EJB3Subsystem13Parser extends EJB3Subsystem12Parser {

    protected EJB3Subsystem13Parser() {
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
                    EJB3SubsystemRootResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
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
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.CONNECTOR_REF, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        final PathAddress ejb3RemoteServiceAddress = SUBSYSTEM_PATH.append(SERVICE, REMOTE);
        ModelNode operation = Util.createAddOperation(ejb3RemoteServiceAddress);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CONNECTOR_REF:
                    EJB3RemoteResourceDefinition.CONNECTOR_REF.parseAndSetParameter(value, operation, reader);
                    break;
                case THREAD_POOL_NAME:
                    EJB3RemoteResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        operation.get(EJB3SubsystemModel.EXECUTE_IN_WORKER).set(new ModelNode(false));
        operations.add(operation);

        // set the address for this operation
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

    protected void parseChannelCreationOptions(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> operations) throws XMLStreamException {
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

    protected void parseChannelCreationOption(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> operations) throws XMLStreamException {
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.TYPE);
        final int count = reader.getAttributeCount();
        String optionName = null;
        ModelNode operation = Util.createAddOperation();
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
                    RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.parseAndSetParameter(attributeValue, operation, reader);
                    break;
                case VALUE:
                    RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.parseAndSetParameter(attributeValue, operation, reader);
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
        operation.get(ADDRESS).set(address.append(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS, optionName).toModelNode());
        operations.add(operation);
    }

}
