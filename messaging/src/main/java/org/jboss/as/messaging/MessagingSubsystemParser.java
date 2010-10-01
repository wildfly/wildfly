package org.jboss.as.messaging;

import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 */
public class MessagingSubsystemParser implements
        XMLElementReader<List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>>> {

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

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader,
            List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates) throws XMLStreamException {

        final MessagingSubsystemUpdate update = new MessagingSubsystemUpdate();
        updates.add(update);

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
                    processAcceptors(reader, updates);
                    break;
                case ADDRESS_SETTINGS:
                    // add address settings
                    processAddressSettings(reader, updates);
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
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setBindingsDirectory(text.trim());
                    }
                }
                    break;
                case BROADCAST_PERIOD:
                    unhandledElement(reader, element);
                    break;
                case CLUSTERED: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setClustered(Boolean.getBoolean(text.trim()));
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
                    processConnectors(reader, updates);
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
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setJournalDirectory(text.trim());
                    }
                }
                    break;
                case JOURNAL_MIN_FILES: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setJournalMinFiles(Integer.valueOf(text.trim()));
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
                        update.setJournalType(jtype);
                    }
                }
                    break;
                case JOURNAL_FILE_SIZE: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        int size = Integer.valueOf(text.trim());
                        update.setJournalFileSize(size);
                    }
                }
                    break;
                case JOURNAL_MAX_IO:
                    unhandledElement(reader, element);
                    break;
                case LARGE_MESSAGES_DIRECTORY: {
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setLargeMessagesDirectory(text.trim());
                    }
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
                    String text = reader.getElementText();
                    if (text != null && text.length() > 0) {
                        update.setPagingDirectory(text.trim());
                    }
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
                    processSecuritySettings(reader, updates);
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

        // Set the log delegate
        // config.setLogDelegateFactoryClassName();
        log.tracef("End %s:%s", reader.getLocation(), reader.getLocalName());
    }

    /**
     * Process acceptors.
     *
     * @param reader the stream reader
     * @param updates the updates
     * @throws XMLStreamException
     */
    /*
        <acceptor name="netty">
           <factory-class>org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory</factory-class>
           <param key="host"  value="${jboss.bind.address:localhost}"/>
           <param key="port"  value="${hornetq.remoting.netty.port:5445}"/>
        </acceptor>
     */
    void processAcceptors(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates) throws XMLStreamException {
        String localName = null;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPTOR:
                    String name = reader.getAttributeValue(0);
                    TransportConfiguration acceptorConfig = ElementUtils.parseTransportConfiguration(reader, name, Element.ACCEPTOR);
                    // Add acceptor
                    updates.add(new AcceptorAddUpdate(acceptorConfig));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ACCEPTOR.getLocalName()));
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
    void processAddressSettings(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates) throws XMLStreamException {
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDRESS_SETTING:
                    String match = reader.getAttributeValue(0);
                    Pair<String, AddressSettings> settings = ElementUtils.parseAddressSettings(reader, match);
                    // Add address settings
                    updates.add(new AddressSettingAddUpdate(settings));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));
    }

    /*
    <connector name="netty">
       <factory-class>org.hornetq.core.remoting.impl.netty.NettyConnectorFactory</factory-class>
       <param key="host"  value="${jboss.bind.address:localhost}"/>
       <param key="port"  value="${hornetq.remoting.netty.port:5445}"/>
    </connector>
     */
    void processConnectors(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates) throws XMLStreamException {
        // Handle elements
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR:
                    String name = reader.getAttributeValue(0);
                    TransportConfiguration connectorConfig = ElementUtils.parseTransportConfiguration(reader, name, Element.CONNECTOR);
                    updates.add(new ConnectorAddUpdate(connectorConfig));
                    break;
            }
        } while (reader.hasNext() && localName.equals(org.jboss.as.messaging.Element.CONNECTOR.getLocalName()));
    }

    /*
        <security-setting match="#">
            <permission type="createNonDurableQueue" roles="guest"/>
            <permission type="deleteNonDurableQueue" roles="guest"/>
            <permission type="consume" roles="guest"/>
            <permission type="send" roles="guest"/>
        </security-setting>
     */
    void processSecuritySettings(XMLExtendedStreamReader reader,
            List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> updates) throws XMLStreamException {
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());

            switch (element) {
                case SECURITY_SETTING:
                    String match = reader.getAttributeValue(0);
                    Pair<String, Set<Role>> roles = ElementUtils.parseSecurityRoles(reader, match);
                    updates.add(new SecurityRolesAddUpdate(roles));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
    }

    static void unhandledElement(final XMLExtendedStreamReader reader, final Element element) throws XMLStreamException {
        log.warnf("Ignorning unhandled element: %s, at: %s", element, reader.getLocation().toString());
        reader.discardRemainder();
    }
}
