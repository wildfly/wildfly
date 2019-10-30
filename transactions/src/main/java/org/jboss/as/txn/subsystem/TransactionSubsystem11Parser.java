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

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 */
class TransactionSubsystem11Parser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    TransactionSubsystem11Parser() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);

        list.add(subsystem);

        // elements
        final EnumSet<Element> required = EnumSet.of(Element.RECOVERY_ENVIRONMENT, Element.CORE_ENVIRONMENT);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case TRANSACTIONS_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    required.remove(element);
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case RECOVERY_ENVIRONMENT: {
                            parseRecoveryEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case CORE_ENVIRONMENT: {
                            parseCoreEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case COORDINATOR_ENVIRONMENT: {
                            parseCoordinatorEnvironmentElement(reader, subsystem);
                            break;
                        }
                        case OBJECT_STORE: {
                            parseObjectStoreEnvironmentElementAndEnrichOperation(reader, subsystem);
                            break;
                        }
                        case JTS: {
                            parseJts(reader, subsystem);
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
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
        final ModelNode logStoreAddress = address.clone();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        logStoreAddress.add(LogStoreConstants.LOG_STORE, LogStoreConstants.LOG_STORE);

        logStoreAddress.protect();

        operation.get(OP_ADDR).set(logStoreAddress);
        list.add(operation);

    }

    private void parseJts(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        operation.get(CommonAttributes.JTS).set(true);
        requireNoContent(reader);
    }

    static void parseObjectStoreEnvironmentElementAndEnrichOperation(final XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);

    }

    static void parseCoordinatorEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLE_STATISTICS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.parseAndSetParameter(value, operation, reader);
                    break;
                case ENABLE_TSM_STATUS:
                    TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.parseAndSetParameter(value, operation, reader);
                    break;
                case DEFAULT_TIMEOUT:
                    TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);

    }

    /**
     * Handle the core-environment element and children
     *
     * @param reader the stream reader
     * @param operation ModelNode for the core-environment add operation
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    static void parseCoreEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NODE_IDENTIFIER:
                    TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                case RELATIVE_TO:
                    throw TransactionLogger.ROOT_LOGGER.unsupportedAttribute(attribute.getLocalName(), reader.getLocation());
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // elements
        final EnumSet<Element> required = EnumSet.of(Element.PROCESS_ID);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case PROCESS_ID: {
                    if (!encountered.add(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    parseProcessIdEnvironmentElement(reader, operation);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
    }

    /**
     * Handle the process-id child elements
     *
     * @param reader  the stream reader
     * @param coreEnvironmentAdd the add operation model node
     *
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    static void parseProcessIdEnvironmentElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        // elements
        boolean encountered = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case UUID:
                    if (encountered) {
                        throw unexpectedElement(reader);
                    }
                    encountered = true;
                    if (reader.getAttributeCount() > 0) {
                        throw unexpectedAttribute(reader, 0);
                    }
                    coreEnvironmentAdd.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).set(true);
                    requireNoContent(reader);
                    break;
                case SOCKET: {
                    if (encountered) {
                        throw unexpectedElement(reader);
                    }
                    encountered = true;
                    parseSocketProcessIdElement(reader, coreEnvironmentAdd);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        if (!encountered) {
            throw missingOneOf(reader, EnumSet.of(Element.UUID, Element.SOCKET));
        }
    }

    static void parseSocketProcessIdElement(XMLExtendedStreamReader reader, ModelNode coreEnvironmentAdd) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case BINDING:
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.parseAndSetParameter(value, coreEnvironmentAdd, reader);
                    break;
                case SOCKET_PROCESS_ID_MAX_PORTS:
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.parseAndSetParameter(value, coreEnvironmentAdd, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);
    }

    static void parseRecoveryEnvironmentElement(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {

        Set<Attribute> required = EnumSet.of(Attribute.BINDING, Attribute.STATUS_BINDING);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case BINDING:
                    TransactionSubsystemRootResourceDefinition.BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                case STATUS_BINDING:
                    TransactionSubsystemRootResourceDefinition.STATUS_BINDING.parseAndSetParameter(value, operation, reader);
                    break;
                case RECOVERY_LISTENER:
                    TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);

    }
}
