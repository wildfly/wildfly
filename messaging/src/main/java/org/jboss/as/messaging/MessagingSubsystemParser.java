package org.jboss.as.messaging;

import static org.jboss.as.model.ParseUtils.unexpectedAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.config.impl.Validators;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.jboss.as.ExtensionContext;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 */
public class MessagingSubsystemParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<MessagingSubsystemElement>>> {

    private static final Logger log = Logger.getLogger("org.jboss.as.messaging");
    private static final MessagingSubsystemParser INSTANCE = new MessagingSubsystemParser();

    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private MessagingSubsystemParser() {
        //
    }

    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<ExtensionContext.SubsystemConfiguration<MessagingSubsystemElement>> result) throws XMLStreamException {

        final List<AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<MessagingSubsystemElement,?>>();

        final MessagingSubsystemAdd messagingSubsystemAdd = new MessagingSubsystemAdd();

        // Handle elements
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            log.tracef("%s -> %s, event=%s", localName, element, ElementUtils.StaxEvent.tagToEvent(tag));
            switch (element) {
                case ACCEPTORS:
                    // add acceptors
                    processAcceptors(reader, messagingSubsystemAdd);
                    break;
                case ADDRESS_SETTINGS:
                    // add address settings
                    processAddressSettings(reader, messagingSubsystemAdd);
                    break;
                case ASYNC_CONNECTION_EXECUTION_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case BACKUP:
                    unhandledElement(reader, element);
                    break;
                case BACKUP_CONNECTOR_REF:
                    unhandledElement(reader, element);
                    break;
                case BINDINGS_DIRECTORY: {
                    final DirectoryElement dir = parseDirectory(reader);
                    messagingSubsystemAdd.setBindingsDirectory(dir);
                }
                    break;
                case BROADCAST_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case CLUSTERED: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        messagingSubsystemAdd.setClustered(Boolean.getBoolean(text.trim()));
                    }
                }
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
                case CONNECTORS:
                    // process connectors
                    processConnectors(reader, messagingSubsystemAdd);
                    break;
                case CONNECTOR_REF:
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
                    final DirectoryElement dir = parseDirectory(reader);
                    messagingSubsystemAdd.setJournalDirectory(dir);
                }
                    break;
                case JOURNAL_MIN_FILES: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        messagingSubsystemAdd.setJournalMinFiles(Integer.valueOf(text.trim()));
                    }
                }
                    break;
                case JOURNAL_SYNC_NON_TRANSACTIONAL:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_SYNC_TRANSACTIONAL:
                    unhandledElement(reader, element);
                    break;
                case JOURNAL_TYPE: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        JournalType jtype = JournalType.valueOf(text.trim());
                        messagingSubsystemAdd.setJournalType(jtype);
                    }
                }
                    break;
                case JOURNAL_FILE_SIZE: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        int size = Integer.valueOf(text.trim());
                        messagingSubsystemAdd.setJournalFileSize(size);
                    }
                }
                    break;
                case JOURNAL_MAX_IO:
                    unhandledElement(reader, element);
                    break;
                case LARGE_MESSAGES_DIRECTORY: {
                    final DirectoryElement dir = parseDirectory(reader);
                    messagingSubsystemAdd.setLargeMessagesDirectory(dir);
                }
                    break;
                case LOCAL_BIND_ADDRESS:
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
                    final DirectoryElement dir = parseDirectory(reader);
                    messagingSubsystemAdd.setPagingDirectory(dir);
                }
                    break;
                case PERF_BLAST_PAGES:
                    unhandledElement(reader, element);
                    break;
                case PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY:
                    unhandledElement(reader, element);
                    break;
                case PERSIST_ID_CACHE:
                    unhandledElement(reader, element);
                    break;
                case PERSISTENCE_ENABLED:
                    unhandledElement(reader, element);
                    break;
                case REFRESH_TIMEOUT:
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
                case SECURITY_SETTINGS:
                    // process security settings
                    processSecuritySettings(reader, messagingSubsystemAdd);
                    break;
                case SERVER_DUMP_INTERVAL:
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
                case SUBSYSTEM:
                    // The end of the subsystem element
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        } while (reader.hasNext() && localName.equals("subsystem") == false);

        result.setResult(new ExtensionContext.SubsystemConfiguration<MessagingSubsystemElement>(messagingSubsystemAdd, updates));

        // Set the log delegate
        // config.setLogDelegateFactoryClassName();
        log.tracef("End %s:%s", reader.getLocation(), reader.getLocalName());
    }

    /**
     * Process acceptors.
     *
     * @param reader the stream reader
     * @param messagingSubsystemAdd the messaging subsystem add
     * @throws XMLStreamException
     */
    /*
        <acceptor name="netty">
           <factory-class>org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory</factory-class>
           <param key="host"  value="${jboss.bind.address:localhost}"/>
           <param key="port"  value="${hornetq.remoting.netty.port:5445}"/>
        </acceptor>
     */
    void processAcceptors(XMLExtendedStreamReader reader, final MessagingSubsystemAdd messagingSubsystemAdd) throws XMLStreamException {
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
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NETTY_ACCEPTOR: {
                    final NettyAcceptorSpec acceptor = new NettyAcceptorSpec(name);
                    acceptor.setSocketBindingRef(socketBinding);
                    parseTransportConfigurationParams(reader, acceptor, element);
                    messagingSubsystemAdd.addAcceptor(acceptor);
                    break;
                } case IN_VM_ACCEPTOR: {
                    final InVMAcceptorSpec acceptor = new InVMAcceptorSpec(name);
                    acceptor.setServerId(serverId);
                    parseTransportConfigurationParams(reader, acceptor, element);
                    messagingSubsystemAdd.addAcceptor(acceptor);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }


    /*
    <address-settings>
       <!--default for catch all-->
       <address-setting match="#">
          <dead-letter-address>jms.queue.DLQ</dead-letter-address>
          <expiry-address>jms.queue.ExpiryQueue</expiry-address>
          <redelivery-delay>0</redelivery-delay>
          <max-size-bytes>10485760</max-size-bytes>
          <message-counter-history-day-limit>10</message-counter-history-day-limit>
          <address-full-policy>BLOCK</address-full-policy>
       </address-setting>
    </address-settings>
     */
    void processAddressSettings(final XMLExtendedStreamReader reader, final MessagingSubsystemAdd messagingSubsystemAdd) throws XMLStreamException {
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDRESS_SETTING:
                    String match = reader.getAttributeValue(0);
                    final AddressSettingsSpecification addressSettingsSpec = parseAddressSettings(reader, match);
                    // Add address settings
                    messagingSubsystemAdd.addAddressSettings(addressSettingsSpec);
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));
    }

    static AddressSettingsSpecification parseAddressSettings(final XMLExtendedStreamReader reader, String name) throws XMLStreamException {
        final AddressSettingsSpecification addressSettingsSpec = new AddressSettingsSpecification(name);

        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());

            switch (element) {
                case DEAD_LETTER_ADDRESS_NODE_NAME: {
                    SimpleString queueName = new SimpleString(reader.getElementText().trim());
                    addressSettingsSpec.setDeadLetterAddress(queueName);
                }
                    break;
                case EXPIRY_ADDRESS_NODE_NAME: {
                    SimpleString queueName = new SimpleString(reader.getElementText().trim());
                    addressSettingsSpec.setExpiryAddress(queueName);
                }
                    break;
                case REDELIVERY_DELAY_NODE_NAME: {
                    addressSettingsSpec.setRedeliveryDelay(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MAX_SIZE_BYTES_NODE_NAME: {
                    addressSettingsSpec.setMaxSizeBytes(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case PAGE_SIZE_BYTES_NODE_NAME: {
                    addressSettingsSpec.setPageSizeBytes(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME: {
                    addressSettingsSpec.setMessageCounterHistoryDayLimit(Integer.valueOf(reader.getElementText().trim()));
                }
                    break;
                case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME: {
                    String value = reader.getElementText().trim();
                    Validators.ADDRESS_FULL_MESSAGE_POLICY_TYPE.validate(
                            Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME.getLocalName(), value);
                    AddressFullMessagePolicy policy = null;
                    if (value.equals(AddressFullMessagePolicy.BLOCK.toString())) {
                        policy = AddressFullMessagePolicy.BLOCK;
                    } else if (value.equals(AddressFullMessagePolicy.DROP.toString())) {
                        policy = AddressFullMessagePolicy.DROP;
                    } else if (value.equals(AddressFullMessagePolicy.PAGE.toString())) {
                        policy = AddressFullMessagePolicy.PAGE;
                    }
                    addressSettingsSpec.setAddressFullMessagePolicy(policy);
                }
                    break;
                case LVQ_NODE_NAME: {
                    addressSettingsSpec.setLastValueQueue(Boolean.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MAX_DELIVERY_ATTEMPTS: {
                    addressSettingsSpec.setMaxDeliveryAttempts(Integer.valueOf(reader.getElementText().trim()));
                }
                    break;
                case REDISTRIBUTION_DELAY_NODE_NAME: {
                    addressSettingsSpec.setRedistributionDelay(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case SEND_TO_DLA_ON_NO_ROUTE: {
                    addressSettingsSpec.setSendToDLAOnNoRoute(Boolean.valueOf(reader.getElementText().trim()));
                }
                    break;
                default:
                    break;
            }

            reader.discardRemainder();
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName())
                && reader.getEventType() != XMLExtendedStreamReader.END_ELEMENT);

        return addressSettingsSpec;
    }

    void processConnectors(final XMLExtendedStreamReader reader, final MessagingSubsystemAdd messagingSubsystemAdd) throws XMLStreamException {
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
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NETTY_CONNECTOR: {
                    final NettyConnectorSpec connector = new NettyConnectorSpec(name);
                    connector.setSocketBindingRef(socketBinding);
                    parseTransportConfigurationParams(reader, connector, element);
                    messagingSubsystemAdd.addConnector(connector);
                    break;
                } case IN_VM_CONNECTOR: {
                    final InVMConnectorSpec connector = new InVMConnectorSpec(name);
                    connector.setServerId(serverId);
                    parseTransportConfigurationParams(reader, connector, element);
                    messagingSubsystemAdd.addConnector(connector);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void parseTransportConfigurationParams(final XMLExtendedStreamReader reader, final AbstractTransportSpecification<?> spec, final Element parentElement) throws XMLStreamException {
        Map<String, Object> params = new HashMap<String, Object>();

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {

            String localString = reader.getLocalName();
            if(Element.PARAM.getLocalName().equals(localString) == false) {
                throw ParseUtils.unexpectedElement(reader);
            }

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
            params.put(key, value);
            ParseUtils.requireNoContent(reader);
        }
        spec.setParams(params);
    }

    /*
        <security-setting match="#">
            <permission type="createNonDurableQueue" roles="guest"/>
            <permission type="deleteNonDurableQueue" roles="guest"/>
            <permission type="consume" roles="guest"/>
            <permission type="send" roles="guest"/>
        </security-setting>
     */
    void processSecuritySettings(final XMLExtendedStreamReader reader, final MessagingSubsystemAdd messagingSubsystemAdd) throws XMLStreamException {
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());

            switch (element) {
                case SECURITY_SETTING:
                    String match = reader.getAttributeValue(0);
                    final SecuritySettingsSpecification securitySettingsSpec = parseSecurityRoles(reader, match);
                    messagingSubsystemAdd.addSecuritySettings(securitySettingsSpec);
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
    }

    static void unhandledElement(final XMLExtendedStreamReader reader, final Element element) throws XMLStreamException {
        log.warnf("Ignorning unhandled element: %s, at: %s", element, reader.getLocation().toString());
        reader.discardRemainder();
    }

    SecuritySettingsSpecification parseSecurityRoles(final XMLExtendedStreamReader reader, final String match) throws XMLStreamException {
        final Set<Role> securityRoles = new HashSet<Role>();

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
            if (localName.equals(Element.PERMISSION_ELEMENT_NAME.getLocalName()) == false)
                break;
            final Element element = Element.forName(reader.getLocalName());

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
            securityRoles.add(new Role(role, send.contains(role), consume.contains(role), createDurableQueue.contains(role),
                    deleteDurableQueue.contains(role), createNonDurableQueue.contains(role), deleteNonDurableQueue
                            .contains(role), manageRoles.contains(role)));
        }

        return new SecuritySettingsSpecification(match, securityRoles);
    }

    static DirectoryElement parseDirectory(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final DirectoryElement element = new DirectoryElement();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                final String value = reader.getAttributeValue(i);
                switch (attribute) {
                    case RELATIVE_TO:
                        element.setRelativeTo(value.trim());
                        break;
                    case PATH:
                        element.setPath(value.trim());
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        return element;
    }
}
