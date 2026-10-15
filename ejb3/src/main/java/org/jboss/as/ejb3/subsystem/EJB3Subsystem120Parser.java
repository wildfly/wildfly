/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * Parser for ejb3:12.0 namespace.
 *
 * TODO Parameterize a single parser class by schema version.  Inheritence is a poor model for versioning.
 */
public class EJB3Subsystem120Parser extends EJB3Subsystem110Parser {

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_12_0;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case ACCESS_LOG: {
                parseAccessLog(reader, operations);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    protected void parseAccessLog(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.ACCESS_LOG_PATH);
        final ModelNode operation = Util.createAddOperation(address);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DESTINATION:
                    AccessLogResourceDefinition.DESTINATION.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                    AccessLogResourceDefinition.PATH.parseAndSetParameter(value, operation, reader);
                    break;
                case RELATIVE_TO:
                    AccessLogResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                case ROTATE_SUFFIX:
                    AccessLogResourceDefinition.ROTATE_SUFFIX.parseAndSetParameter(value, operation, reader);
                    break;
                case WORKER:
                    AccessLogResourceDefinition.WORKER.parseAndSetParameter(value, operation, reader);
                    break;
                case INCLUDE_LOCAL:
                    AccessLogResourceDefinition.INCLUDE_LOCAL.parseAndSetParameter(value, operation, reader);
                    break;
                case INCLUDE_NODE_NAME:
                    AccessLogResourceDefinition.INCLUDE_NODE_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case ATTRIBUTES:
                    AccessLogResourceDefinition.ATTRIBUTES.getParser().parseAndSetParameter(AccessLogResourceDefinition.ATTRIBUTES, value, operation, reader);
                    break;
                case QUEUE_LENGTH:
                    AccessLogResourceDefinition.QUEUE_LENGTH.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        final Set<EJB3SubsystemXMLElement> parsedElements = new HashSet<>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
            switch (element) {
                case METADATA: {
                    if (parsedElements.contains(EJB3SubsystemXMLElement.METADATA)) {
                        throw unexpectedElement(reader);
                    }
                    parsedElements.add(EJB3SubsystemXMLElement.METADATA);
                    parseMetadata(reader, operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        operations.add(operation);
    }

    protected void parseMetadata(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final EJB3SubsystemXMLElement element = EJB3SubsystemXMLElement.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    parseProperty(reader, operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseProperty(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        String name = null;
        String value = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = attrValue;
                    break;
                case VALUE:
                    value = attrValue;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        if (value == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.VALUE.getLocalName()));
        }
        AccessLogResourceDefinition.METADATA.parseAndAddParameterElement(name, value, operation, reader);
        org.jboss.as.controller.parsing.ParseUtils.requireNoContent(reader);
    }
}
