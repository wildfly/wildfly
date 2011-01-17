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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.messaging.CommonAttributes.*;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class NewMessagingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

    private static final NewMessagingSubsystemParser INSTANCE = new NewMessagingSubsystemParser();

    public static NewMessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private NewMessagingSubsystemParser() {
        //
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

        final ModelNode address = new ModelNode().setEmptyObject();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        final ModelNode requestProperties = subsystem.get(REQUEST_PROPERTIES);

        // Handle elements
        String localName = null;
        do {
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPTORS: {
                    // add acceptors
                    final ModelNode acceptors = processAcceptors(reader);
                    requestProperties.get(ACCEPTOR).set(acceptors);
                    break;
                } case ADDRESS_SETTINGS: {
                    // add address settings
                    final ModelNode addressSettings = processAddressSettings(reader);
                    requestProperties.get(ADDRESS_SETTING).set(addressSettings);
                    break;
                } case ASYNC_CONNECTION_EXECUTION_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case BACKUP:
                    unhandledElement(reader, element);
                    break;
                case BACKUP_CONNECTOR_REF:
                    unhandledElement(reader, element);
                    break;
                case BINDINGS_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    requestProperties.get(BINDINGS_DIRECTORY).set(directory);
                    break;
                } case BROADCAST_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case CLUSTERED:
                    requestProperties.get(CLUSTERED).set(reader.getElementText());
                    break;
                case CLUSTER_PASSWORD:
                    unhandledElement(reader, element);
                    break;
                case CLUSTER_USER:
                    unhandledElement(reader, element);
                    break;
                case CONNECTION_TTL_OVERRIDE:
                    unhandledElement(reader, element);
                    break;
                case CONNECTORS: {
                    final ModelNode connectors = processConnectors(reader);
                    requestProperties.get(CONNECTOR).set(connectors);
                    break;
                } case CONNECTOR_REF:
                    unhandledElement(reader, element);
                    break;
                case CREATE_BINDINGS_DIR:
                    unhandledElement(reader, element);
                    break;
                case CREATE_JOURNAL_DIR:
                    unhandledElement(reader, element);
                    break;
                case FILE_DEPLOYMENT_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case GROUP_ADDRESS:
                    unhandledElement(reader, element);
                    break;
                case GROUP_PORT:
                    unhandledElement(reader, element);
                    break;
                case GROUPING_HANDLER:
                    unhandledElement(reader, element);
                    break;
                case ID_CACHE_SIZE:
                    unhandledElement(reader, element);
                    break;
                case JMX_DOMAIN:
                    unhandledElement(reader, element);
                    break;
                case JMX_MANAGEMENT_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_BUFFER_SIZE:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_BUFFER_TIMEOUT:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_COMPACT_MIN_FILES:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_COMPACT_PERCENTAGE:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    requestProperties.get(JOURNAL_DIRECTORY).set(directory);
                    break;
                }
                case JOURNAL_MIN_FILES: {
                    requestProperties.get(JOURNAL_MIN_FILES).set(reader.getElementText());
                    break;
                } case JOURNAL_SYNC_NON_TRANSACTIONAL:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_SYNC_TRANSACTIONAL:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_TYPE: {
                    String journalType = reader.getElementText();
                    if (journalType != null && journalType.length() > 0) {
                        requestProperties.get(JOURNAL_TYPE).set(journalType.trim());
                    }
                    break;
                } case JOURNAL_FILE_SIZE: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        requestProperties.get(JOURNAL_FILE_SIZE).set(text.trim());
                    }
                }
                    break;
                case JOURNAL_MAX_IO:
                    unhandledElement(reader, element);
                    break;
                case LARGE_MESSAGES_DIRECTORY: {
                    final ModelNode dir = parseDirectory(reader);
                    requestProperties.get(LARGE_MESSAGES_DIRECTORY).set(dir);
                    break;
                } case LOCAL_BIND_ADDRESS:
                    unhandledElement(reader, element);
                    break;
                case LOCAL_BIND_PORT:
                    unhandledElement(reader, element);
                    break;
                case LOG_JOURNAL_WRITE_RATE:
                    unhandledElement(reader, element);
                    break;
                case MANAGEMENT_ADDRESS:
                    unhandledElement(reader, element);
                    break;
                case MANAGEMENT_NOTIFICATION_ADDRESS:
                    unhandledElement(reader, element);
                    break;
                case MEMORY_MEASURE_INTERVAL:
                    unhandledElement(reader, element);
                    break;
                case MEMORY_WARNING_THRESHOLD:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_COUNTER_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_COUNTER_MAX_DAY_HISTORY:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_COUNTER_SAMPLE_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_EXPIRY_SCAN_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_EXPIRY_THREAD_PRIORITY:
                    unhandledElement(reader, element);
                    break;
                case PAGING_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    requestProperties.get(PAGING_DIRECTORY).set(directory);
                    break;
                } case PERF_BLAST_PAGES:
                    unhandledElement(reader, element);
                    break;
                case PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY:
                    unhandledElement(reader, element);
                    break;
                case PERSIST_ID_CACHE:
                    unhandledElement(reader, element);
                    break;
                case PERSISTENCE_ENABLED: {
                    final String value = reader.getElementText();
                    if(value != null && value.length() > 0) {
                        boolean enabled = Boolean.valueOf(value.trim());
                        requestProperties.get(PERSISTENCE_ENABLED).set(enabled);
                    }
                    break;
                } case REFRESH_TIMEOUT:
                    unhandledElement(reader, element);
                    break;
                case REMOTING_INTERCEPTORS:
                    unhandledElement(reader, element);
                    break;
                case RUN_SYNC_SPEED_TEST:
                    unhandledElement(reader, element);
                    break;
                case SECURITY_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case SECURITY_INVALIDATION_INTERVAL:
                    unhandledElement(reader, element);
                    break;
                case SECURITY_SETTINGS: {
                    // process security settings
                    final ModelNode securitySettings = processSecuritySettings(reader);
                    requestProperties.get(SECURITY_SETTING).set(securitySettings);
                    break;
                } case SERVER_DUMP_INTERVAL:
                    unhandledElement(reader, element);
                    break;
                case SHARED_STORE:
                    unhandledElement(reader, element);
                    break;
                case TRANSACTION_TIMEOUT:
                    unhandledElement(reader, element);
                    break;
                case TRANSACTION_TIMEOUT_SCAN_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case WILD_CARD_ROUTING_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case DEAD_LETTER_ADDRESS_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case EXPIRY_ADDRESS_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case REDELIVERY_DELAY_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case MAX_DELIVERY_ATTEMPTS:
                    unhandledElement(reader, element);
                    break;
                case MAX_SIZE_BYTES_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case PAGE_SIZE_BYTES_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case LVQ_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case REDISTRIBUTION_DELAY_NODE_NAME:
                    unhandledElement(reader, element);
                    break;
                case SEND_TO_DLA_ON_NO_ROUTE:
                    unhandledElement(reader, element);
                    break;
                case QUEUES: {
                    final ModelNode queues = parseQueues(reader);
                    requestProperties.get(QUEUE).set(queues);
                    break;
                } case SUBSYSTEM:
                    // The end of the subsystem element
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        } while (reader.hasNext() && localName.equals("subsystem") == false);
    }


    static ModelNode parseQueues(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode queues = new ModelNode();

        return queues;
    }

    static ModelNode processSecuritySettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode security = new ModelNode();


        return security;
    }


    static ModelNode processConnectors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();



        return connectors;
    }

    static ModelNode processAddressSettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode settings = new ModelNode();


        return settings;
    }

    static ModelNode processAcceptors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode acceptors = new ModelNode();


        return acceptors;
    }

    static ModelNode parseDirectory(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode directory = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                final String value = reader.getAttributeValue(i);
                switch (attribute) {
                    case RELATIVE_TO:
                        directory.get(RELATIVE_TO).set(value);
                        break;
                    case PATH:
                        directory.get(PATH).set(value);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        return directory;
    }

    static void unhandledElement(XMLExtendedStreamReader reader, Element element) throws XMLStreamException {
        throw new XMLStreamException(String.format("Ignorning unhandled element: %s, at: %s", element, reader.getLocation().toString()));
    }

}
