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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.messaging.CommonAttributes.*;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.hornetq.core.server.JournalType;
import org.jboss.as.messaging.MessagingServices.TransportConfigType;
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
    public void writeContent(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {

        writer.writeEndElement();
    }

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, NewMessagingExtension.SUBSYSTEM_NAME);
        list.add(operation);

        // Handle elements
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPTORS: {
                    // add acceptors
                    final ModelNode acceptors = processAcceptors(reader);
                    operation.get(ACCEPTOR).set(acceptors);
                    break;
                } case ADDRESS_SETTINGS: {
                    // add address settings
                    final ModelNode addressSettings = processAddressSettings(reader);
                    operation.get(ADDRESS_SETTING).set(addressSettings);
                    break;
                } case ASYNC_CONNECTION_EXECUTION_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case BACKUP:
                    handleElementText(reader, element, operation);
                    break;
                case BACKUP_CONNECTOR_REF:
                    handleElementText(reader, element, operation);
                    break;
                case BINDINGS_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    operation.get(BINDINGS_DIRECTORY).set(directory);
                    break;
                } case BROADCAST_PERIOD:
                    handleElementText(reader, element, operation);
                    break;
                case CLUSTERED:
                    handleElementText(reader, element, operation);
                    break;
                case CLUSTER_PASSWORD:
                    handleElementText(reader, element, operation);
                    break;
                case CLUSTER_USER:
                    handleElementText(reader, element, operation);
                    break;
                case CONNECTION_TTL_OVERRIDE:
                    handleElementText(reader, element, operation);
                    break;
                case CONNECTORS: {
                    final ModelNode connectors = processConnectors(reader);
                    operation.get(CONNECTOR).set(connectors);
                    break;
                } case CONNECTOR_REF:
                    unhandledElement(reader, element);
                    break;
                case CREATE_BINDINGS_DIR:
                    handleElementText(reader, element, operation);
                    break;
                case CREATE_JOURNAL_DIR:
                    handleElementText(reader, element, operation);
                    break;
                case FILE_DEPLOYMENT_ENABLED:
                    unhandledElement(reader, element); // no filesystem support in AS
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
                    handleElementText(reader, element, operation);
                    break;
                case JMX_DOMAIN:
                    handleElementText(reader, element, operation);
                    break;
                case JMX_MANAGEMENT_ENABLED:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_BUFFER_SIZE:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_BUFFER_TIMEOUT:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_COMPACT_MIN_FILES:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_COMPACT_PERCENTAGE:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    operation.get(JOURNAL_DIRECTORY).set(directory);
                    break;
                }
                case JOURNAL_MIN_FILES: {
                    operation.get(JOURNAL_MIN_FILES).set(reader.getElementText());
                    break;
                } case JOURNAL_SYNC_NON_TRANSACTIONAL:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_SYNC_TRANSACTIONAL:
                    handleElementText(reader, element, operation);
                    break;
                case JOURNAL_TYPE: {
                    String journalType = reader.getElementText();
                    if (journalType != null && journalType.length() > 0) {
                        JournalType.valueOf(journalType.trim());
                        operation.get(JOURNAL_TYPE).set(journalType.trim());
                    }
                    break;
                } case JOURNAL_FILE_SIZE: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        operation.get(JOURNAL_FILE_SIZE).set(text.trim());
                    }
                }
                    break;
                case JOURNAL_MAX_IO:
                    handleElementText(reader, element, operation);
                    break;
                case LARGE_MESSAGES_DIRECTORY: {
                    final ModelNode dir = parseDirectory(reader);
                    operation.get(LARGE_MESSAGES_DIRECTORY).set(dir);
                    break;
                } case LOCAL_BIND_ADDRESS:
                    unhandledElement(reader, element);
                    break;
                case LOCAL_BIND_PORT:
                    unhandledElement(reader, element);
                    break;
                case LOG_JOURNAL_WRITE_RATE:
                    handleElementText(reader, element, operation);
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
                    operation.get(PAGING_DIRECTORY).set(directory);
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
                        operation.get(PERSISTENCE_ENABLED).set(enabled);
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
                    operation.get(SECURITY_SETTING).set(securitySettings);
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
                    operation.get(QUEUE).set(queues);
                    break;
                } case SUBSYSTEM:
                    // The end of the subsystem element
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        } while (reader.hasNext() && localName.equals("subsystem") == false);
    }

    static ModelNode processAcceptors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode acceptors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            int serverId = 0;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    }
                    case SERVER_ID: {
                        serverId = Integer.valueOf(attrValue);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.Generic.toString());
                    if(socketBinding != null) acceptor.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, acceptor, true);
                    break;
                } case NETTY_ACCEPTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.Remote.toString());
                    if(socketBinding != null) acceptor.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, acceptor, false);
                    break;
                } case IN_VM_ACCEPTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.InVM.toString());
                    acceptor.get(SERVER_ID).set(serverId);
                    parseTransportConfigurationParams(reader, acceptor, false);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return acceptors;
    }

    static ModelNode parseQueues(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode queues = new ModelNode();
        queues.setEmptyObject();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case QUEUE: {
                    if(name == null) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME.getLocalName()));
                    }
                    parseQueue(reader, queues.get(name));
                    if(! queues.get(name).has(ADDRESS)) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Element.ADDRESS.getLocalName()));
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return queues;
    }

    static void parseQueue(final XMLExtendedStreamReader reader, final ModelNode queue) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDRESS: {
                    queue.get(CommonAttributes.ADDRESS).set(reader.getElementText().trim());
                    break;
                } case FILTER: {
                    queue.get(FILTER).set(reader.getAttributeValue(0).trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } case DURABLE: {
                    queue.get(DURABLE).set(Boolean.valueOf(reader.getElementText()));
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static ModelNode processSecuritySettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode security = new ModelNode();
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());

            switch (element) {
                case SECURITY_SETTING:
                    String match = reader.getAttributeValue(0);
                    parseSecurityRoles(reader, security.get(match));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
        return security;
    }

    static void parseSecurityRoles(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {

        ArrayList<String> send = new ArrayList<String>();
        ArrayList<String> consume = new ArrayList<String>();
        ArrayList<String> createDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteDurableQueue = new ArrayList<String>();
        ArrayList<String> createNonDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
        ArrayList<String> manageRoles = new ArrayList<String>();
        ArrayList<String> allRoles = new ArrayList<String>();

        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element != Element.PERMISSION_ELEMENT_NAME) {
                break;
            }

            List<String> roles = null;
            String type = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case ROLES_ATTR_NAME:
                            roles = reader.getListAttributeValue(i);
                            break;
                        case TYPE_ATTR_NAME:
                            type = reader.getAttributeValue(i);
                            break;
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            for (String role : roles) {
                if (Attribute.SEND_NAME.getLocalName().equals(type)) {
                    send.add(role.trim());
                } else if (Attribute.CONSUME_NAME.getLocalName().equals(type)) {
                    consume.add(role.trim());
                } else if (Attribute.CREATEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    createDurableQueue.add(role);
                } else if (Attribute.DELETEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    deleteDurableQueue.add(role);
                } else if (Attribute.CREATE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.CREATETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.MANAGE_NAME.getLocalName().equals(type)) {
                    manageRoles.add(role);
                }
                if (!allRoles.contains(role.trim())) {
                    allRoles.add(role.trim());
                }
            }
            // Scan to element end
            reader.discardRemainder();
        } while (reader.hasNext());

        for (String role : allRoles) {
            node.get(role, SEND_NAME).set(send.contains(role));
            node.get(role, CONSUME_NAME).set(consume.contains(role));
            node.get(role, CREATEDURABLEQUEUE_NAME).set(createDurableQueue.contains(role));
            node.get(role, DELETEDURABLEQUEUE_NAME).set(deleteDurableQueue.contains(role));
            node.get(role, CREATE_NON_DURABLE_QUEUE_NAME).set(createNonDurableQueue.contains(role));
            node.get(role, DELETE_NON_DURABLE_QUEUE_NAME).set(deleteNonDurableQueue.contains(role));
            node.get(role, MANAGE_NAME).set(manageRoles.contains(role));
        }
    }

    static ModelNode processConnectors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            int serverId = 0;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    }
                    case SERVER_ID: {
                        serverId = Integer.valueOf(attrValue);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.Generic.toString());
                    if(socketBinding != null) connector.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, connector, true);
                    break;
                } case NETTY_CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.Remote.toString());
                    if(socketBinding != null) connector.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, connector, false);
                    break;
                } case IN_VM_CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.InVM.toString());
                    connector.get(SERVER_ID).set(serverId);
                    parseTransportConfigurationParams(reader, connector, false);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return connectors;
    }

    static ModelNode processAddressSettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode settings = new ModelNode();

        String localName = null;
        int tag = reader.getEventType();
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            switch (element) {
                case ADDRESS_SETTING:
                    // Add address settings
                    String match = reader.getAttributeValue(0);
                    settings.get(match).set(parseAddressSettings(reader));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));

        return settings;
    }

    static ModelNode parseAddressSettings(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode addressSettingsSpec = new ModelNode();

        String localName = null;
        int tag;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case DEAD_LETTER_ADDRESS_NODE_NAME:
                case EXPIRY_ADDRESS_NODE_NAME:
                case REDELIVERY_DELAY_NODE_NAME:
                case MAX_SIZE_BYTES_NODE_NAME:
                case PAGE_SIZE_BYTES_NODE_NAME:
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME:
                case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME:
                case LVQ_NODE_NAME:
                case MAX_DELIVERY_ATTEMPTS:
                case REDISTRIBUTION_DELAY_NODE_NAME:
                case SEND_TO_DLA_ON_NO_ROUTE: {
                    handleElementText(reader, element, addressSettingsSpec);
                    break;
                } default: {
                    break;
                }
            }
            reader.discardRemainder();
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName())
                && reader.getEventType() != XMLExtendedStreamReader.END_ELEMENT);

        return addressSettingsSpec;
    }

    static void parseTransportConfigurationParams(final XMLExtendedStreamReader reader, final ModelNode transportConfig, final boolean generic) throws XMLStreamException {
        final ModelNode params = new ModelNode();

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            int count = reader.getAttributeCount();
            String key = null;
            String value = null;
            for (int n = 0; n < count; n++) {
                String attrName = reader.getAttributeLocalName(n);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case KEY:
                        key = reader.getAttributeValue(n);
                        break;
                    case VALUE:
                        value = reader.getAttributeValue(n);
                        break;
                    default:
                        throw unexpectedAttribute(reader, n);
                }
            }

            Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case FACTORY_CLASS: {
                    if(! generic) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    transportConfig.get(FACTORY_CLASS).set(reader.getElementText().trim());
                    break;
                }
                case PARAM: {
                    params.add(key, value);
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        transportConfig.get(PARAM).set(params);
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

    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node) throws XMLStreamException {
        final String value = reader.getElementText();
        if(value != null && value.length() > 0) {
            node.get(element.getLocalName()).set(value.trim());
        }
    }

    static void writeSimpleElement(final XMLExtendedStreamWriter writer, final Element element, final ModelNode node) throws XMLStreamException {
        final String localName = element.getLocalName();
        if(node.has(localName)) {
            final String content = node.get(localName).asString();
            if(content != null) {
                writer.writeStartElement(localName);
                writer.writeCharacters(content);
                writer.writeEndElement();
            }
        }
    }

    static void writeDirectory(final XMLExtendedStreamWriter writer, final Element element, final ModelNode node) throws XMLStreamException {
        final String localName = element.getLocalName();
        if(node.has(localName)) {
            final String path = node.get(localName).has(PATH) ? node.get(localName, PATH).asString() : null;
            final String relativeTo = node.get(localName).has(RELATIVE_TO) ? node.get(localName, RELATIVE_TO).asString() : null;
            if(path != null || relativeTo != null) {
                writer.writeEmptyElement(localName);
                if(path != null) writer.writeAttribute(PATH, path);
                if(relativeTo != null) writer.writeAttribute(RELATIVE_TO, relativeTo);
            }
        }
    }

}
