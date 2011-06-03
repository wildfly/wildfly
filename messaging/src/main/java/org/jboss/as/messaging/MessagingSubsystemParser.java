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
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.messaging.CommonAttributes.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.hornetq.core.server.JournalType;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.messaging.MessagingServices.TransportConfigType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class MessagingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final MessagingSubsystemParser INSTANCE = new MessagingSubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private MessagingSubsystemParser() {
        //
    }


    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
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
                case CORE_QUEUES: {
                    final ModelNode queues = parseQueues(reader);
                    operation.get(QUEUE).set(queues);
                    break;
                } case CONNECTION_FACTORIES: {
                    processConnectionFactories(reader, address, list);
                    break;
                } case JMS_DESTINATIONS: {
                    processJmsDestinations(reader, address, list);
                    break;
                } case SUBSYSTEM:
                    // The end of the subsystem element
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        } while (reader.hasNext() && localName.equals("subsystem") == false);
    }

    static void processConnectionFactories(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
       while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
          final Element element = Element.forName(reader.getLocalName());
          switch(element) {
             case CONNECTION_FACTORY:
                processConnectionFactory(reader, address, updates);
               break;
             case POOLED_CONNECTION_FACTORY:
                processPooledConnectionFactory(reader, address, updates);
               break;
             default:
                    throw ParseUtils.unexpectedElement(reader);
          }
       }
    }

   static void processJmsDestinations(final XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> updates) throws XMLStreamException {
       while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
          final Element element = Element.forName(reader.getLocalName());
          switch(element) {
             case JMS_QUEUE:
                processJMSQueue(reader, address, updates);
               break;
             case JMS_TOPIC:
                processJMSTopic(reader, address, updates);
               break;
             default:
                    throw ParseUtils.unexpectedElement(reader);
          }
       }
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
                case ACCEPTOR: {
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
                requireNoNamespaceAttribute(reader, i);
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

    public static ModelNode processConnectors(XMLExtendedStreamReader reader) throws XMLStreamException {
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

            SWITCH:
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
                    break SWITCH;
                } default: {
                    break;
                }
            }
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName()) && reader.getEventType() == XMLExtendedStreamReader.END_ELEMENT);

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

            requireNoNamespaceAttribute(reader, i);
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
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
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

    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode node = context.getModelNode();
        if (has(node, ACCEPTOR)) {
            writeAcceptors(writer, node.get(ACCEPTOR));
        }
        if (has(node, ADDRESS_SETTING)) {
            writeAddressSettings(writer, node.get(ADDRESS_SETTING));
        }
        if (has(node, ASYNC_CONNECTION_EXECUTION_ENABLED)) {
            //unhandled
        }
        if (has(node, BACKUP)) {
            writeSimpleElement(writer, Element.BACKUP, node);
        }
        if (has(node, CONNECTOR_REF)) {
            writeSimpleElement(writer, Element.CONNECTOR_REF, node);
        }
        if (has(node, BINDINGS_DIRECTORY)) {
            writeDirectory(writer, Element.BINDINGS_DIRECTORY, node);
        }
        if (has(node, BROADCAST_PERIOD)) {
            writeSimpleElement(writer, Element.BROADCAST_PERIOD, node);
        }
        if (has(node, CLUSTERED)) {
            writeSimpleElement(writer, Element.CLUSTERED, node);
        }
        if (has(node, CLUSTER_PASSWORD)){
            writeSimpleElement(writer, Element.CLUSTER_PASSWORD, node);
        }
        if (has(node, CLUSTER_USER)) {
            writeSimpleElement(writer, Element.CLUSTER_USER, node);
        }
        if (has(node, CONNECTION_TTL_OVERRIDE)) {
            writeSimpleElement(writer, Element.CONNECTION_TTL_OVERRIDE, node);
        }
        if (has(node, CONNECTOR)) {
            writeConnectors(writer, node.get(CONNECTOR));
        }
        if (has(node, CONNECTOR_REF)) {
            //unhandled
        }
        if (has(node, CommonAttributes.CREATE_BINDINGS_DIR)) {
            writeSimpleElement(writer, Element.CREATE_BINDINGS_DIR, node);
        }
        if (has(node, CommonAttributes.CREATE_JOURNAL_DIR)) {
            writeSimpleElement(writer, Element.CREATE_BINDINGS_DIR, node);
        }
        if (has(node, CommonAttributes.FILE_DEPLOYMENT_ENABLED)) {
            //unhandled
        }
        if (has(node, CommonAttributes.GROUP_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.GROUP_PORT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.GROUPING_HANDLER)) {
            //unhandled
        }
        if (has(node, CommonAttributes.ID_CACHE_SIZE)) {
            writeSimpleElement(writer, Element.ID_CACHE_SIZE, node);
        }
        if (has(node, CommonAttributes.JMX_DOMAIN)) {
            writeSimpleElement(writer, Element.JMX_DOMAIN, node);
        }
        if (has(node, CommonAttributes.JMX_MANAGEMENT_ENABLED)) {
            writeSimpleElement(writer, Element.JMX_MANAGEMENT_ENABLED, node);
        }
        if (has(node, CommonAttributes.JOURNAL_BUFFER_SIZE)) {
            //unhandled
        }
        if (has(node, CommonAttributes.JOURNAL_BUFFER_TIMEOUT)) {
            writeSimpleElement(writer, Element.JOURNAL_BUFFER_TIMEOUT, node);
        }
        if (has(node, CommonAttributes.JOURNAL_COMPACT_MIN_FILES)) {
            writeSimpleElement(writer, Element.JOURNAL_COMPACT_MIN_FILES, node);
        }
        if (has(node, CommonAttributes.JOURNAL_COMPACT_PERCENTAGE)) {
            writeSimpleElement(writer, Element.JOURNAL_COMPACT_PERCENTAGE, node);
        }
        if (has(node, CommonAttributes.JOURNAL_COMPACT_PERCENTAGE)) {
            writeSimpleElement(writer, Element.JOURNAL_COMPACT_PERCENTAGE, node);
        }
        if (has(node, CommonAttributes.JOURNAL_DIRECTORY)) {
            writeDirectory(writer, Element.JOURNAL_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.JOURNAL_MIN_FILES)) {
            writeSimpleElement(writer, Element.JOURNAL_MIN_FILES, node);
        }
        if (has(node, CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL)) {
            writeSimpleElement(writer, Element.JOURNAL_SYNC_TRANSACTIONAL, node);
        }
        if (has(node, CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL)) {
            writeSimpleElement(writer, Element.JOURNAL_SYNC_TRANSACTIONAL, node);
        }
        if (has(node, CommonAttributes.JOURNAL_TYPE)) {
            writeSimpleElement(writer, Element.JOURNAL_TYPE, node);
        }
        if (has(node, CommonAttributes.JOURNAL_FILE_SIZE)) {
            writeSimpleElement(writer, Element.JOURNAL_FILE_SIZE, node);
        }
        if (has(node, CommonAttributes.JOURNAL_MAX_IO)) {
            writeSimpleElement(writer, Element.JOURNAL_MAX_IO, node);
        }
        if (has(node, CommonAttributes.LARGE_MESSAGES_DIRECTORY)) {
            writeDirectory(writer, Element.LARGE_MESSAGES_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.LOCAL_BIND_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.LOCAL_BIND_PORT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.LOG_JOURNAL_WRITE_RATE)) {
            writeSimpleElement(writer, Element.LOG_JOURNAL_WRITE_RATE, node);
        }
        if (has(node, CommonAttributes.MANAGEMENT_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MEMORY_MEASURE_INTERVAL)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MEMORY_WARNING_THRESHOLD)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_COUNTER_ENABLED)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.PAGING_DIRECTORY)) {
            writeDirectory(writer, Element.PAGING_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.PERF_BLAST_PAGES)) {
            //unhandled
        }
        if (has(node, CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.PERSIST_ID_CACHE)) {
            //unhandled
        }
        if (has(node, CommonAttributes.PERSISTENCE_ENABLED)) {
            writeSimpleElement(writer, Element.PERSISTENCE_ENABLED, node);
        }
        if (has(node, CommonAttributes.REFRESH_TIMEOUT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.REMOTING_INTERCEPTORS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.RUN_SYNC_SPEED_TEST)) {
            //unhandled
        }
        if (has(node, CommonAttributes.SECURITY_ENABLED)) {
            //unhandled
        }
        if (has(node, CommonAttributes.SECURITY_INVALIDATION_INTERVAL)) {
            //unhandled
        }
        if (has(node, CommonAttributes.SECURITY_SETTING)) {
            writeSecuritySettings(writer, node.get(CommonAttributes.SECURITY_SETTING));
        }
        if (has(node, CommonAttributes.SERVER_DUMP_INTERVAL)) {
            //unhandled
        }
        if (has(node, CommonAttributes.SHARED_STORE)) {
            //unhandled
        }
        if (has(node, CommonAttributes.TRANSACTION_TIMEOUT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD)) {
            //unhandled
        }
        if (has(node, CommonAttributes.WILD_CARD_ROUTING_ENABLED)) {
            //unhandled
        }
        if (has(node, CommonAttributes.DEAD_LETTER_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.EXPIRY_ADDRESS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.REDELIVERY_DELAY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MAX_DELIVERY_ATTEMPTS)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MAX_SIZE_BYTES_NODE_NAME)) {
            //unhandled
        }
        if (has(node, CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME)) {
            //unhandled
        }
        if (has(node, CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT)) {
            //unhandled
        }
        if (has(node, CommonAttributes.LVQ)) {
            //unhandled
        }
        if (has(node, CommonAttributes.REDISTRIBUTION_DELAY)) {
            //unhandled
        }
        if (has(node, CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE)) {
            //unhandled
        }
        if (has(node, CommonAttributes.QUEUE)) {
            writeQueues(writer, node.get(CommonAttributes.QUEUE));
        }
        if (node.has(CONNECTION_FACTORY) || node.has(POOLED_CONNECTION_FACTORY)) {
           writer.writeStartElement(CONNECTION_FACTORIES);
           if (node.has(CONNECTION_FACTORY)) {
               writeConnectionFactories(writer, node.get(CONNECTION_FACTORY));
           }
           if (node.has(POOLED_CONNECTION_FACTORY)) {
               writePooledConnectionFactories(writer, node.get(POOLED_CONNECTION_FACTORY));
           }
           writer.writeEndElement();
        }
        if (node.has(JMS_QUEUE) || node.has(JMS_TOPIC)) {
           writer.writeStartElement(JMS_DESTINATIONS);
           if(node.has(JMS_QUEUE)) {
            writeJmsQueues(writer, node.get(JMS_QUEUE));
           }
           if (node.has(JMS_TOPIC)) {
               writeTopics(writer, node.get(JMS_TOPIC));
           }
           writer.writeEndElement();
        }


        writer.writeEndElement();
    }

    private void writeAcceptors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ACCEPTORS.getLocalName());
        for(final Property property : node.asPropertyList()) {
            final ModelNode value = property.getValue();
            if (!has(value, TYPE)) {
                continue;
            }
            switch( Enum.valueOf(TransportConfigType.class, value.get(TYPE).asString())) {
                case Generic:
                    writer.writeStartElement(Element.ACCEPTOR.getLocalName());
                    break;
                case Remote:
                    writer.writeStartElement(Element.NETTY_ACCEPTOR.getLocalName());
                    break;
                case InVM:
                    writer.writeStartElement(Element.IN_VM_ACCEPTOR.getLocalName());
                    break;
            }

            writeAcceptorAndConnectorContent(writer, property);

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeConnectors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.CONNECTORS.getLocalName());
        for(final Property property : node.asPropertyList()) {
            final ModelNode value = property.getValue();
            if (!has(value, TYPE)) {
                continue;
            }
            switch( Enum.valueOf(TransportConfigType.class, value.get(TYPE).asString())) {
                case Generic:
                    writer.writeStartElement(Element.CONNECTOR.getLocalName());
                    break;
                case Remote:
                    writer.writeStartElement(Element.NETTY_CONNECTOR.getLocalName());
                    break;
                case InVM:
                    writer.writeStartElement(Element.IN_VM_CONNECTOR.getLocalName());
                    break;
            }
            writeAcceptorAndConnectorContent(writer, property);

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeAcceptorAndConnectorContent(final XMLExtendedStreamWriter writer, final Property property) throws XMLStreamException {
        writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
        final ModelNode value = property.getValue();

        if (has(value, SOCKET_BINDING)) {
            writeAttribute(writer, Attribute.SOCKET_BINDING, value.get(SOCKET_BINDING));
        }
        if (has(value, SERVER_ID)) {
            writeAttribute(writer, Attribute.SERVER_ID, value.get(SERVER_ID));
        }

        if (has(value, FACTORY_CLASS)) {
            writeSimpleElement(writer, Element.FACTORY_CLASS, value);
        }
        if (has(value, PARAM)) {
            for(final Property parameter : value.get(PARAM).asPropertyList()) {
                writer.writeStartElement(Element.PARAM.getLocalName());
                writer.writeAttribute(Attribute.KEY.getLocalName(), parameter.getName());
                writeAttribute(writer, Attribute.VALUE, parameter.getValue());
                writer.writeEndElement();
            }
        }
    }

    private void writeSecuritySettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());

        for (Property matchRoles : node.asPropertyList()) {
            writer.writeStartElement(Element.SECURITY_SETTING.getLocalName());
            writer.writeAttribute(Attribute.MATCH.getLocalName(), matchRoles.getName());

            ArrayList<String> send = new ArrayList<String>();
            ArrayList<String> consume = new ArrayList<String>();
            ArrayList<String> createDurableQueue = new ArrayList<String>();
            ArrayList<String> deleteDurableQueue = new ArrayList<String>();
            ArrayList<String> createNonDurableQueue = new ArrayList<String>();
            ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
            ArrayList<String> manageRoles = new ArrayList<String>();

            for (Property rolePerms : matchRoles.getValue().asPropertyList()) {
                final String role = rolePerms.getName();
                final ModelNode perms = rolePerms.getValue();
                if (perms.get(CommonAttributes.SEND_NAME).asBoolean()) {
                    send.add(role);
                }
                if (perms.get(CommonAttributes.CONSUME_NAME).asBoolean()) {
                    consume.add(role);
                }
                if (perms.get(CommonAttributes.CREATEDURABLEQUEUE_NAME).asBoolean()) {
                    createDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.DELETEDURABLEQUEUE_NAME).asBoolean()) {
                    deleteDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.CREATE_NON_DURABLE_QUEUE_NAME).asBoolean()) {
                    createNonDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.DELETE_NON_DURABLE_QUEUE_NAME).asBoolean()) {
                    deleteNonDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.MANAGE_NAME).asBoolean()) {
                    manageRoles.add(role);
                }
            }

            writePermission(writer, SEND_NAME, send);
            writePermission(writer, CONSUME_NAME, consume);
            writePermission(writer, CREATEDURABLEQUEUE_NAME, createDurableQueue);
            writePermission(writer, DELETEDURABLEQUEUE_NAME, deleteDurableQueue);
            writePermission(writer, CREATE_NON_DURABLE_QUEUE_NAME, createNonDurableQueue);
            writePermission(writer, DELETE_NON_DURABLE_QUEUE_NAME, deleteNonDurableQueue);
            writePermission(writer, MANAGE_NAME, manageRoles);

            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writePermission(final XMLExtendedStreamWriter writer, final String type, final List<String> roles) throws XMLStreamException {
        if (roles.size() == 0) {
            return;
        }
        writer.writeStartElement(Element.PERMISSION_ELEMENT_NAME.getLocalName());
        StringBuilder sb = new StringBuilder();
        for (String role : roles) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(role);
        }
        writer.writeAttribute(Attribute.TYPE_ATTR_NAME.getLocalName(), type);
        writer.writeAttribute(Attribute.ROLES_ATTR_NAME.getLocalName(), sb.toString());
        writer.writeEndElement();
    }

    private void writeAddressSettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
        for (Property matchSetting : node.asPropertyList()) {
            writer.writeStartElement(Element.ADDRESS_SETTING.getLocalName());
            writer.writeAttribute(Attribute.MATCH.getLocalName(), matchSetting.getName());
            final ModelNode setting = matchSetting.getValue();
            if (has(setting, CommonAttributes.DEAD_LETTER_ADDRESS)) {
                writeSimpleElement(writer, Element.DEAD_LETTER_ADDRESS_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.EXPIRY_ADDRESS)) {
                writeSimpleElement(writer, Element.EXPIRY_ADDRESS_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.REDELIVERY_DELAY)) {
                writeSimpleElement(writer, Element.REDELIVERY_DELAY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MAX_SIZE_BYTES_NODE_NAME)) {
                writeSimpleElement(writer, Element.MAX_SIZE_BYTES_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME)) {
                writeSimpleElement(writer, Element.PAGE_SIZE_BYTES_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT)) {
                writeSimpleElement(writer, Element.MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY)) {
                writeSimpleElement(writer, Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.LVQ)) {
                writeSimpleElement(writer, Element.LVQ_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MAX_DELIVERY_ATTEMPTS)) {
                writeSimpleElement(writer, Element.MAX_DELIVERY_ATTEMPTS, setting);
            }
            if (has(setting, CommonAttributes.REDISTRIBUTION_DELAY)) {
                writeSimpleElement(writer, Element.REDISTRIBUTION_DELAY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE)) {
                writeSimpleElement(writer, Element.SEND_TO_DLA_ON_NO_ROUTE, setting);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.CORE_QUEUES.getLocalName());
        for (Property queueProp : node.asPropertyList()) {
            writer.writeStartElement(Element.QUEUE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), queueProp.getName());
            final ModelNode queue = queueProp.getValue();
            if (has(queue, ADDRESS)) {
                writeSimpleElement(writer, Element.ADDRESS, queue);
            }
            if (has(queue, FILTER)) {
                writer.writeStartElement(Element.FILTER.getLocalName());
                writeAttribute(writer, Attribute.STRING, queue);
                writer.writeEndElement();
            }
            if (has(queue, DURABLE)) {
                writeSimpleElement(writer, Element.DURABLE, queue);
            }

            writer.writeEndElement();
        }
        writer.writeEndElement();
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


    private void writeConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode factory = prop.getValue();
            if (factory.isDefined()) {
               writer.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
               writer.writeAttribute(Attribute.NAME.getLocalName(), name);
               writeConnectionFactory(writer, node, name, factory);
            }
        }
    }

    private void writePooledConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode factory = prop.getValue();
            if (factory.isDefined()) {
               writer.writeStartElement(Element.POOLED_CONNECTION_FACTORY.getLocalName());
               writer.writeAttribute(Attribute.NAME.getLocalName(), name);
               writeConnectionFactory(writer, node, name, factory);
            }
        }
    }

    private void writeConnectionFactory(XMLExtendedStreamWriter writer, ModelNode node, String name, ModelNode factory) throws XMLStreamException
    {
        if (has(factory, CommonAttributes.DISCOVERY_GROUP_REF)) {
            writer.writeStartElement(Element.DISCOVERY_GROUP_REF.getLocalName());
            writeAttribute(writer, Attribute.DISCOVERY_GROUP_NAME, factory.get(DISCOVERY_GROUP_REF));
            writer.writeEndElement();
        }
        if (has(factory, CONNECTOR)) {
            writer.writeStartElement(Element.CONNECTORS.getLocalName());
            for (Property connProp : factory.get(CONNECTOR).asPropertyList()) {
                final ModelNode conn = connProp.getValue();
                if (conn.isDefined()) {
                    writer.writeStartElement(Element.CONNECTOR_REF.getLocalName());
                    writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), connProp.getName());
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        if (has(factory, ENTRIES)) {
            final ModelNode entries = factory.get(ENTRIES);
            if (entries.getType() == ModelType.LIST) {
                writer.writeStartElement(Element.ENTRIES.getLocalName());
                for (ModelNode entry : entries.asList()) {
                    if (entry.isDefined()) {
                        writer.writeStartElement(Element.ENTRY.getLocalName());
                        writeAttribute(writer, Attribute.NAME, entry);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }
        }
        if(has(node, TRANSACTION)) {
            writer.writeStartElement(Element.TRANSACTION.getLocalName());
            writeTransactionTypeAttribute(writer, Element.MODE, node.get(TRANSACTION));
            writer.writeEndElement();
        }
        if(has(factory, INBOUND_CONFIG)) {
            final ModelNode inboundConfigs = factory.get(INBOUND_CONFIG);
            if (inboundConfigs.getType() == ModelType.LIST) {
                writer.writeStartElement(Element.INBOUND_CONFIG.getLocalName());
                for (ModelNode config : inboundConfigs.asList()) {
                    if (config.isDefined()) {
                        if(has(config, CommonAttributes.USE_JNDI)) {
                            writeSimpleElement(writer, Element.USE_JNDI, config);
                        }
                        if(has(config, CommonAttributes.JNDI_PARAMS)) {
                            writeSimpleElement(writer, Element.JNDI_PARAMS, config);
                        }
                        if(has(config, CommonAttributes.SETUP_ATTEMPTS)) {
                            writeSimpleElement(writer, Element.SETUP_ATTEMPTS, config);
                        }
                        if(has(config, CommonAttributes.SETUP_INTERVAL)) {
                            writeSimpleElement(writer, Element.SETUP_INTERVAL, config);
                        }
                        if(has(config, CommonAttributes.USE_LOCAL_TX)) {
                            writeSimpleElement(writer, Element.USE_LOCAL_TX, config);
                        }
                    }
                }
                writer.writeEndElement();
            }
        }
        //ENTRIES

        if (has(factory, CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT)){
            writeSimpleElement(writer, Element.DISCOVERY_INITIAL_WAIT_TIMEOUT, node);
        }
        if (has(factory, CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD)){
            writeSimpleElement(writer, Element.DISCOVERY_INITIAL_WAIT_TIMEOUT, node);
        }
        if (has(factory, CommonAttributes.CONNECTION_TTL)){
            writeSimpleElement(writer, Element.CONNECTION_TTL, node);
        }
        if (has(factory, CommonAttributes.CALL_TIMEOUT)){
            writeSimpleElement(writer, Element.CALL_TIMEOUT, node);
        }
        if (has(factory, CommonAttributes.CONSUMER_WINDOW_SIZE)){
            writeSimpleElement(writer, Element.CONSUMER_WINDOW_SIZE, node);
        }
        if (has(factory, CommonAttributes.CONSUMER_MAX_RATE)){
            writeSimpleElement(writer, Element.CONSUMER_MAX_RATE, node);
        }
        if (has(factory, CommonAttributes.CONFIRMATION_WINDOW_SIZE)){
            writeSimpleElement(writer, Element.CONFIRMATION_WINDOW_SIZE, node);
        }
        if (has(factory, CommonAttributes.PRODUCER_WINDOW_SIZE)){
            writeSimpleElement(writer, Element.PRODUCER_WINDOW_SIZE, node);
        }
        if (has(factory, CommonAttributes.PRODUCER_MAX_RATE)){
            writeSimpleElement(writer, Element.PRODUCER_MAX_RATE, node);
        }
        if (has(factory, CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT)){
            writeSimpleElement(writer, Element.CACHE_LARGE_MESSAGE_CLIENT, node);
        }
        if (has(factory, CommonAttributes.MIN_LARGE_MESSAGE_SIZE)){
            writeSimpleElement(writer, Element.MIN_LARGE_MESSAGE_SIZE, node);
        }
        if (has(factory, CommonAttributes.CLIENT_ID)){
            writeSimpleElement(writer, Element.CLIENT_ID, node);
        }
        if (has(factory, CommonAttributes.DUPS_OK_BATCH_SIZE)){
            writeSimpleElement(writer, Element.DUPS_OK_BATCH_SIZE, node);
        }
        if (has(factory, CommonAttributes.TRANSACTION_BATCH_SIZE)){
            writeSimpleElement(writer, Element.TRANSACTION_BATH_SIZE, node);
        }
        if (has(factory, CommonAttributes.BLOCK_ON_ACK)){
            writeSimpleElement(writer, Element.BLOCK_ON_ACK, node);
        }
        if (has(factory, CommonAttributes.BLOCK_ON_NON_DURABLE_SEND)){
            writeSimpleElement(writer, Element.BLOCK_ON_NON_DURABLE_SEND, node);
        }
        if (has(factory, CommonAttributes.BLOCK_ON_DURABLE_SEND)){
            writeSimpleElement(writer, Element.BLOCK_ON_DURABLE_SEND, node);
        }
        if (has(factory, CommonAttributes.AUTO_GROUP)){
            writeSimpleElement(writer, Element.AUTO_GROUP, node);
        }
        if (has(factory, CommonAttributes.PRE_ACK)){
            writeSimpleElement(writer, Element.PRE_ACK, node);
        }
        if (has(factory, CommonAttributes.RETRY_INTERVAL_MULTIPLIER)){
            writeSimpleElement(writer, Element.RETRY_INTERVAL_MULTIPLIER, node);
        }
        if (has(factory, CommonAttributes.MAX_RETRY_INTERVAL)){
            writeSimpleElement(writer, Element.MAX_RETRY_INTERVAL, node);
        }
        if (has(factory, CommonAttributes.RECONNECT_ATTEMPTS)){
            writeSimpleElement(writer, Element.RECONNECT_ATTEMPTS, node);
        }
        if (has(factory, CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION)){
            writeSimpleElement(writer, Element.FAILOVER_ON_INITIAL_CONNECTION, node);
        }
        if (has(factory, CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN)){
            writeSimpleElement(writer, Element.FAILOVER_ON_SERVER_SHUTDOWN, node);
        }
        if (has(factory, CommonAttributes.LOAD_BALANCING_CLASS_NAME)){
            writeSimpleElement(writer, Element.LOAD_BALANCING_CLASS_NAME, node);
        }
        if (has(factory, CommonAttributes.USE_GLOBAL_POOLS)){
            writeSimpleElement(writer, Element.USE_GLOBAL_POOLS, node);
        }
        if (has(factory, CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE)){
            writeSimpleElement(writer, Element.SCHEDULED_THREAD_POOL_MAX_SIZE, node);
        }
        if (has(factory, CommonAttributes.THREAD_POOL_MAX_SIZE)){
            writeSimpleElement(writer, Element.THREAD_POOL_MAX_SIZE, node);
        }
        if (has(factory, CommonAttributes.GROUP_ID)){
            writeSimpleElement(writer, Element.GROUP_ID, node);
        }

        writer.writeEndElement();
    }

    private void writeJmsQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode queue = prop.getValue();
            if (queue.isDefined()) {
                writer.writeStartElement(Element.JMS_QUEUE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                if (queue.has(CommonAttributes.ENTRIES)) {
                    final ModelNode entries = queue.get(ENTRIES);
                    if (entries.getType() == ModelType.LIST) {
                        for (ModelNode entry : entries.asList()) {
                            if (entry.isDefined()) {
                                writer.writeStartElement(Element.ENTRY.getLocalName());
                                writeAttribute(writer, Attribute.NAME, entry);
                                writer.writeEndElement();
                            }
                        }
                    }
                }
                if (has(queue, DURABLE)) {
                    writeSimpleElement(writer, Element.DURABLE, queue);
                }
                if (has(queue, SELECTOR)) {
                    writeSimpleElement(writer, Element.SELECTOR, queue);
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeTopics(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode topic = prop.getValue();
            if (topic.isDefined()) {
                writer.writeStartElement(Element.JMS_TOPIC.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                if (topic.has(CommonAttributes.ENTRIES)) {
                    final ModelNode entries = topic.get(ENTRIES);
                    if (entries.getType() == ModelType.LIST) {
                        for (ModelNode entry : entries.asList()) {
                            if (entry.isDefined()) {
                                writer.writeStartElement(Element.ENTRY.getLocalName());
                                writeAttribute(writer, Attribute.NAME, entry);
                                writer.writeEndElement();
                            }
                        }
                    }
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeTransactionTypeAttribute(final XMLExtendedStreamWriter writer, final Element attr, final ModelNode value) throws XMLStreamException {
        String xaType = value.asString();
        final String txSupport;
        if(LOCAL_TX.equals(xaType)) {
            txSupport = LOCAL_TX;
        } else if (CommonAttributes.NONE.equals(xaType)) {
             txSupport = NO_TX;
        } else {
            txSupport = XA_TX;
        }
        writer.writeAttribute(attr.getLocalName(), txSupport);
    }
    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }


    static void processJMSTopic(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();
        topic.get(OP).set(ADD);
        topic.get(OP_ADDR).set(address).add(JMS_TOPIC, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    topic.get(ENTRIES).add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        updates.add(topic);
    }

    static void processJMSQueue(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode queue = new ModelNode();
        queue.get(OP).set(ADD);
        queue.get(OP_ADDR).set(address).add(JMS_QUEUE, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    queue.get(ENTRIES).add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } case SELECTOR: {
                    if(queue.has(SELECTOR)) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    queue.get(SELECTOR).set(reader.getElementText().trim());
                    break;
                } case DURABLE: {
                    if(queue.has(DURABLE)) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.DURABLE.getLocalName());
                    }
                    queue.get(DURABLE).set(Boolean.valueOf(reader.getElementText()));
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
       updates.add(queue);
    }

    static void processConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory));
    }

    static void processPooledConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(POOLED_CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory));
    }

    static ModelNode processJmsConnectors(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case CONNECTOR_NAME: {
                        name = value.trim();
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.CONNECTOR_NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            if(element != Element.CONNECTOR_REF) {
                throw ParseUtils.unexpectedElement(reader);
            }
            ParseUtils.requireNoContent(reader);
            final ModelNode connector = connectors.get(name).setEmptyObject();
        }
        return connectors;
    }

    private static ModelNode createConnectionFactory(XMLExtendedStreamReader reader, ModelNode connectionFactory) throws XMLStreamException
    {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case DISCOVERY_GROUP_REF: {
                    final String groupRef = reader.getAttributeValue(0);
                    if(groupRef != null) {
                        connectionFactory.get(DISCOVERY_GROUP_REF).add(groupRef);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                } case CONNECTORS: {
                    connectionFactory.get(CONNECTOR).set(processJmsConnectors(reader));
                    break;
                } case ENTRIES: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        if(local != Element.ENTRY ) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        final String entry = reader.getAttributeValue(0);
                        connectionFactory.get(ENTRIES).add(entry.trim());
                        ParseUtils.requireNoContent(reader);
                    }
                    break;
                } case INBOUND_CONFIG: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        switch (local) {
                            case USE_JNDI:
                            case JNDI_PARAMS:
                            case USE_LOCAL_TX:
                            case SETUP_ATTEMPTS:
                            case SETUP_INTERVAL:
                                parseElementText(reader, local, connectionFactory);
                                break;
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                } case TRANSACTION: {
                    final String txType = reader.getAttributeValue(0);
                    if( txType != null) {
                        connectionFactory.get(TRANSACTION).set(txType);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case DISCOVERY_INITIAL_WAIT_TIMEOUT:
                case CLIENT_FAILURE_CHECK_PERIOD:
                case CONNECTION_TTL:
                case CALL_TIMEOUT:
                case CONSUMER_WINDOW_SIZE:
                case CONSUMER_MAX_RATE:
                case CONFIRMATION_WINDOW_SIZE:
                case PRODUCER_WINDOW_SIZE:
                case PRODUCER_MAX_RATE:
                case CACHE_LARGE_MESSAGE_CLIENT:
                case MIN_LARGE_MESSAGE_SIZE:
                case CLIENT_ID:
                case DUPS_OK_BATCH_SIZE:
                case TRANSACTION_BATH_SIZE:
                case BLOCK_ON_ACK:
                case BLOCK_ON_NON_DURABLE_SEND:
                case BLOCK_ON_DURABLE_SEND:
                case AUTO_GROUP:
                case PRE_ACK:
                case RETRY_INTERVAL:
                case RETRY_INTERVAL_MULTIPLIER:
                case MAX_RETRY_INTERVAL:
                case RECONNECT_ATTEMPTS:
                case FAILOVER_ON_INITIAL_CONNECTION:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case LOAD_BALANCING_CLASS_NAME:
                case USE_GLOBAL_POOLS:
                case SCHEDULED_THREAD_POOL_MAX_SIZE:
                case THREAD_POOL_MAX_SIZE:
                case GROUP_ID:
                    parseElementText(reader, element, connectionFactory);
                    break;
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return connectionFactory;
    }


    private static void parseElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node) throws XMLStreamException {
        final String value = reader.getElementText();
        if(value != null && value.length() > 0) {
            node.get(element.getLocalName()).set(value.trim());
        }
    }

}
