package org.jboss.as.messaging;

import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * Parse the HornetQ domain configuration.
 *
 * @author scott.stark@jboss.org
 * @version $Id$
 */
public class ConfigurationElement extends AbstractModelElement<ConfigurationElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   Configuration config = new ConfigurationImpl();


   public ConfigurationElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      log.tracef("Begin %s:%s", reader.getLocation(), reader.getLocalName());
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
            AcceptorsElement acceptors = new AcceptorsElement(reader, config);
            break;
         case ADDRESS_SETTINGS:
            AddressSettingsElement ase = new AddressSettingsElement(reader, config);
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
               config.setBindingsDirectory(text.trim());
            }
         }
         break;
         case BROADCAST_PERIOD:
            unhandledElement(reader, element);
            break;
         case CLUSTERED: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setClustered(Boolean.getBoolean(text.trim()));
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
            ConnectorsElement connectors = new ConnectorsElement(reader, config);
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
               config.setJournalDirectory(text.trim());
            }
         }
         break;
         case JOURNAL_MIN_FILES: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setJournalMinFiles(Integer.valueOf(text.trim()));
            }
         }
         break;
         case JOURNAL_SYNC_NON_TRANSACTIONAL:
            unhandledElement(reader, element);
            break;
         case JOURNAL_SYNC_TRANSACTIONAL:
            unhandledElement(reader, element);
            break;
         case JOURNAL_TYPE:{
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               JournalType jtype = JournalType.valueOf(text.trim());
               config.setJournalType(jtype);
            }
         }
         break;
         case JOURNAL_FILE_SIZE:{
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               int size = Integer.valueOf(text.trim());
               config.setJournalFileSize(size);
            }
         }
         break;
         case JOURNAL_MAX_IO:
            unhandledElement(reader, element);
            break;
         case LARGE_MESSAGES_DIRECTORY: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setLargeMessagesDirectory(text.trim());
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
               config.setPagingDirectory(text.trim());
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
            SecuritySettingsElement sse = new SecuritySettingsElement(reader, config);
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
            throw unexpectedElement(reader);
         }
      } while (reader.hasNext() && localName.equals("subsystem") == false);

      // Set the log delegate
      //config.setLogDelegateFactoryClassName();
      log.tracef("End %s:%s", reader.getLocation(), reader.getLocalName());
   }

   public Configuration getConfiguration() {
      return config;
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<ConfigurationElement> getElementClass() {
      return ConfigurationElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

       Set<TransportConfiguration> acceptors = config.getAcceptorConfigurations();
       if (acceptors != null && acceptors.size() > 0) {
           streamWriter.writeStartElement(Element.ACCEPTORS.getLocalName());
           for (TransportConfiguration transportConfig : acceptors) {
               streamWriter.writeStartElement(Element.ACCEPTOR.getLocalName());
               ElementUtils.writeTransportConfiguration(transportConfig, streamWriter);
               streamWriter.writeEndElement();
           }
           streamWriter.writeEndElement();
       }

       Map<String, AddressSettings> addresses = config.getAddressesSettings();
       if (addresses != null && addresses.size() > 0) {
           streamWriter.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
           for (Map.Entry<String, AddressSettings> entry : addresses.entrySet()) {
               streamWriter.writeStartElement(Element.ADDRESS_SETTING.getLocalName());

               // TODO move this detail out of this class
               streamWriter.writeAttribute(Attribute.MATCH.getLocalName(), entry.getKey());
               AddressSettings addressSettings = entry.getValue();
               ElementUtils.writeSimpleElement(Element.DEAD_LETTER_ADDRESS_NODE_NAME, addressSettings.getDeadLetterAddress(), streamWriter);
               ElementUtils.writeSimpleElement(Element.EXPIRY_ADDRESS_NODE_NAME, addressSettings.getExpiryAddress(), streamWriter);
               ElementUtils.writeSimpleElement(Element.REDELIVERY_DELAY_NODE_NAME, String.valueOf(addressSettings.getRedeliveryDelay()), streamWriter);
               ElementUtils.writeSimpleElement(Element.MAX_SIZE_BYTES_NODE_NAME, String.valueOf(addressSettings.getMaxSizeBytes()), streamWriter);
               ElementUtils.writeSimpleElement(Element.PAGE_SIZE_BYTES_NODE_NAME, String.valueOf(addressSettings.getPageSizeBytes()), streamWriter);
               ElementUtils.writeSimpleElement(Element.MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME, String.valueOf(addressSettings.getMessageCounterHistoryDayLimit()), streamWriter);
               AddressFullMessagePolicy policy = addressSettings.getAddressFullMessagePolicy();
               if (policy != null) {
                   ElementUtils.writeSimpleElement(Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME, policy.toString(), streamWriter);
                }
               ElementUtils.writeSimpleElement(Element.LVQ_NODE_NAME, String.valueOf(addressSettings.isLastValueQueue()), streamWriter);
               ElementUtils.writeSimpleElement(Element.MAX_DELIVERY_ATTEMPTS, String.valueOf(addressSettings.getMaxDeliveryAttempts()), streamWriter);
               ElementUtils.writeSimpleElement(Element.REDISTRIBUTION_DELAY_NODE_NAME, String.valueOf(addressSettings.getRedistributionDelay()), streamWriter);
               ElementUtils.writeSimpleElement(Element.SEND_TO_DLA_ON_NO_ROUTE, String.valueOf(addressSettings.isSendToDLAOnNoRoute()), streamWriter);

               streamWriter.writeEndElement();
           }
           streamWriter.writeEndElement();
       }

       Map<String, TransportConfiguration> connectors = config.getConnectorConfigurations();
       if (connectors != null && connectors.size() > 0) {
           streamWriter.writeStartElement(Element.CONNECTORS.getLocalName());
           for (Map.Entry<String, TransportConfiguration> entry : connectors.entrySet()) {
               streamWriter.writeStartElement(Element.CONNECTOR.getLocalName());
               ElementUtils.writeTransportConfiguration(entry.getValue(), streamWriter);
               streamWriter.writeEndElement();
           }
           streamWriter.writeEndElement();
       }

       Map<String, Set<Role>> roles = config.getSecurityRoles();
       if (roles != null && roles.size() > 0) {
           streamWriter.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());
           for (Map.Entry<String, Set<Role>> entry : roles.entrySet()) {
               streamWriter.writeStartElement(Element.SECURITY_SETTING.getLocalName());

               streamWriter.writeAttribute(Attribute.MATCH.getLocalName(), entry.getKey());
               ElementUtils.writeRoles(entry.getValue(), streamWriter);

               streamWriter.writeEndElement();
           }
           streamWriter.writeEndElement();
       }

       ElementUtils.writeSimpleElement(Element.BINDINGS_DIRECTORY, config.getBindingsDirectory(), streamWriter);

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.CLUSTERED, String.valueOf(config.isClustered()), streamWriter);

       ElementUtils.writeSimpleElement(Element.JOURNAL_DIRECTORY, config.getJournalDirectory(), streamWriter);

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.JOURNAL_MIN_FILES, String.valueOf(config.getJournalMinFiles()), streamWriter);

       JournalType jt = config.getJournalType();
       if (jt != null) {
           ElementUtils.writeSimpleElement(Element.JOURNAL_TYPE, jt.toString(), streamWriter);
       }

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.JOURNAL_FILE_SIZE, String.valueOf(config.getJournalFileSize()), streamWriter);

       ElementUtils.writeSimpleElement(Element.LARGE_MESSAGES_DIRECTORY, config.getLargeMessagesDirectory(), streamWriter);

       ElementUtils.writeSimpleElement(Element.PAGING_DIRECTORY, config.getPagingDirectory(), streamWriter);
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   private void unhandledElement(final XMLExtendedStreamReader reader, final Element element) throws XMLStreamException {
      log.warnf("Ignorning unhandled element: %s, at: %s", element, reader.getLocation().toString());
      reader.discardRemainder();
   }
}
