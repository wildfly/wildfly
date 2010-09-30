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

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The transaction subsystem parser.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
final class TransactionSubsystemElementParser implements XMLStreamConstants, XMLElementReader<List<? super AbstractSubsystemUpdate<TransactionsSubsystemElement, ?>>> {

    private static final TransactionSubsystemElementParser INSTANCE = new TransactionSubsystemElementParser();

    private TransactionSubsystemElementParser() {
        //
    }

    public static TransactionSubsystemElementParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<TransactionsSubsystemElement, ?>> updates)
            throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }

        RecoveryEnvironmentElement recoveryEnvironmentElement = null;
        CoreEnvironmentElement coreEnvironmentElement = null;
        CoordinatorEnvironmentElement coordinatorEnvironmentElement = null;
        ObjectStoreEnvironmentElement objectStoreEnvironmentElement = null;

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case TRANSACTIONS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case RECOVERY_ENVIRONMENT: {
                            recoveryEnvironmentElement = parseRecoveryEnvironmentElement(reader);
                            break;
                        }
                        case CORE_ENVIRONMENT: {
                            coreEnvironmentElement = parseCoreEnvironmentElement(reader);
                            break;
                        }
                        case COORDINATOR_ENVIRONMENT: {
                            coordinatorEnvironmentElement = parseCoordinatorEnvironmentElement(reader);
                            break;
                        }
                        case OBJECT_STORE: {
                            objectStoreEnvironmentElement = parseObjectStoreEnvironmentElement(reader);
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
        if(recoveryEnvironmentElement == null) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.RECOVERY_ENVIRONMENT.getLocalName()));
        }
        if(coreEnvironmentElement == null) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.CORE_ENVIRONMENT.getLocalName()));
        }
        if(coordinatorEnvironmentElement == null) {
            coordinatorEnvironmentElement = new CoordinatorEnvironmentElement();
        }
        if(objectStoreEnvironmentElement == null) {
            objectStoreEnvironmentElement = new ObjectStoreEnvironmentElement();
        }

        final TransactionSubsystemElementUpdate update = new TransactionSubsystemElementUpdate();
        update.setRecoveryEnvironmentElement(recoveryEnvironmentElement);
        update.setCoreEnvironmentElement(coreEnvironmentElement);
        update.setCoordinatorEnvironmentElement(coordinatorEnvironmentElement);
        update.setObjectStoreEnvironmentElement(objectStoreEnvironmentElement);
        // Add the transtaction subsystem update
        updates.add(update);
    }

    RecoveryEnvironmentElement parseRecoveryEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        final RecoveryEnvironmentElement element = new RecoveryEnvironmentElement();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BINDING:
                        element.setBindingRef(value);
                        break;
                    case STATUS_BINDING:
                        element.setStatusBindingRef(value);
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if(element.getBindingRef() == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.BINDING));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return element;
    }

    CoreEnvironmentElement parseCoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        final CoreEnvironmentElement element = new CoreEnvironmentElement();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BINDING:
                        element.setBindingRef(value);
                        break;
                    case NODE_IDENTIFIER:
                        element.setNodeIdentifier(value);
                        break;
                    case SOCKET_PROCESS_ID_MAX_PORTS:
                        element.setMaxPorts(Integer.parseInt(value));
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if(element.getBindingRef() == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.BINDING));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return element;
    }

    CoordinatorEnvironmentElement parseCoordinatorEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        final CoordinatorEnvironmentElement element = new CoordinatorEnvironmentElement();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ENABLE_STATISTICS:
                        element.setEnableStatistics(Boolean.parseBoolean(value));
                        break;
                    case DEFAULT_TIMEOUT:
                        element.setDefaultTimeout(Integer.parseInt(value));
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return element;
    }

    ObjectStoreEnvironmentElement parseObjectStoreEnvironmentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ObjectStoreEnvironmentElement element = new ObjectStoreEnvironmentElement();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DIRECTORY:
                        element.setDirectory(value);
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return element;
    }

}
