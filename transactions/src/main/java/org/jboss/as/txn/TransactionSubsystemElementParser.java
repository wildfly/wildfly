/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The transaction subsystem parser.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
final class TransactionSubsystemElementParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<TransactionsSubsystemElement>>> {

    private static final TransactionSubsystemElementParser INSTANCE = new TransactionSubsystemElementParser();

    private TransactionSubsystemElementParser() {
        //
    }

    public static TransactionSubsystemElementParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<ExtensionContext.SubsystemConfiguration<TransactionsSubsystemElement>> result) throws XMLStreamException {

        List<AbstractSubsystemUpdate<TransactionsSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<TransactionsSubsystemElement,?>>();
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        final TransactionSubsystemAdd add = new TransactionSubsystemAdd();

        // elements
        final EnumSet<Element> required = EnumSet.of(Element.RECOVERY_ENVIRONMENT, Element.CORE_ENVIRONMENT);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case TRANSACTIONS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    required.remove(element);
                    if (! encountered.add(element)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    switch (element) {
                        case RECOVERY_ENVIRONMENT: {
                            parseRecoveryEnvironmentElement(reader, add);
                            break;
                        }
                        case CORE_ENVIRONMENT: {
                            parseCoreEnvironmentElement(reader, add);
                            break;
                        }
                        case COORDINATOR_ENVIRONMENT: {
                            parseCoordinatorEnvironmentElement(reader, add);
                            break;
                        }
                        case OBJECT_STORE: {
                            parseObjectStoreEnvironmentElement(reader, add);
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        if (! required.isEmpty()) {
            throw ParseUtils.missingRequiredElement(reader, required);
        }

        result.setResult(new ExtensionContext.SubsystemConfiguration<TransactionsSubsystemElement>(add, updates));
    }

    void parseRecoveryEnvironmentElement(XMLExtendedStreamReader reader, final TransactionSubsystemAdd add) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BINDING:
                        add.setRecoveryBindingName(value);
                        break;
                    case STATUS_BINDING:
                        add.setRecoveryStatusBindingName(value);
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if(add.getBindingName() == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.BINDING));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    CoreEnvironmentElement parseCoreEnvironmentElement(XMLExtendedStreamReader reader, final TransactionSubsystemAdd add) throws XMLStreamException {
        final CoreEnvironmentElement element = new CoreEnvironmentElement();
        final int count = reader.getAttributeCount();
        final EnumSet<Attribute> required = EnumSet.of(Attribute.BINDING);
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case BINDING:
                        add.setBindingName(value);
                        break;
                    case NODE_IDENTIFIER:
                        add.setNodeIdentifier(value);
                        break;
                    case SOCKET_PROCESS_ID_MAX_PORTS:
                        add.setMaxPorts(Integer.parseInt(value));
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            ParseUtils.missingRequired(reader, required);
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return element;
    }

    void parseCoordinatorEnvironmentElement(XMLExtendedStreamReader reader, final TransactionSubsystemAdd add) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLE_STATISTICS:
                        add.setCoordinatorEnableStatistics(Boolean.parseBoolean(value));
                        break;
                    case DEFAULT_TIMEOUT:
                        add.setCoordinatorDefaultTimeout(Integer.parseInt(value));
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    void parseObjectStoreEnvironmentElement(XMLExtendedStreamReader reader, final TransactionSubsystemAdd add) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case RELATIVE_TO:
                        add.setObjectStorePathRef(value);
                        break;
                    case PATH:
                        add.setObjectStoreDirectory(value);
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

}
