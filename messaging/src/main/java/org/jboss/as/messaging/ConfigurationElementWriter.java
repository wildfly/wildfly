package org.jboss.as.messaging;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Writer for the HornetQ domain configuration.
 *
 * @author scott.stark@jboss.org
 */
public class ConfigurationElementWriter implements Serializable, Cloneable, XMLContentWriter, XMLStreamConstants {

   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   private final Configuration config;

    ConfigurationElementWriter(final Configuration configuration) {
        this.config = configuration;
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

}
