package org.jboss.as.messaging;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.Validators;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Set;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class AddressSettingsElement extends AbstractModelElement<AddressSettingsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   public AddressSettingsElement(final Location location) {
      super(location);
   }

   public AddressSettingsElement(final XMLExtendedStreamReader reader, Configuration config) throws XMLStreamException {
      super(reader);
      System.out.println("Begin " + reader.getLocation() + reader.getLocalName());
      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final Element element = Element.forName(reader.getLocalName());
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
         switch (element) {
         case ADDRESS_SETTING:
            String match = reader.getAttributeValue(0);
            Pair<String, AddressSettings> settings = parseAddressSettings(reader, match);
            config.getAddressesSettings().put(settings.a, settings.b);
            break;
         }
      } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<AddressSettingsElement>> target, AddressSettingsElement other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<AddressSettingsElement> getElementClass() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public Set<TransportConfiguration> getTransportConfiguration() {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   public Pair<String, AddressSettings> parseAddressSettings(final XMLExtendedStreamReader reader, String match)
      throws XMLStreamException {
      AddressSettings addressSettings = new AddressSettings();

      Pair<String, AddressSettings> setting = new Pair<String, AddressSettings>(match, addressSettings);

      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final Element element = Element.forName(reader.getLocalName());

         switch(element) {
         case DEAD_LETTER_ADDRESS_NODE_NAME:
         {
            SimpleString queueName = new SimpleString(reader.getElementText());
            addressSettings.setDeadLetterAddress(queueName);
         }
         break;
         case EXPIRY_ADDRESS_NODE_NAME:
         {
            SimpleString queueName = new SimpleString(reader.getElementText());
            addressSettings.setExpiryAddress(queueName);
         }
         break;
         case REDELIVERY_DELAY_NODE_NAME:
         {
            addressSettings.setRedeliveryDelay(Long.valueOf(reader.getElementText()));
         }
         break;
         case MAX_SIZE_BYTES_NODE_NAME:
         {
            addressSettings.setMaxSizeBytes(Long.valueOf(reader.getElementText()));
         }
         break;
         case PAGE_SIZE_BYTES_NODE_NAME:
         {
            addressSettings.setPageSizeBytes(Long.valueOf(reader.getElementText()));
         }
         break;
         case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME:
         {
            addressSettings.setMessageCounterHistoryDayLimit(Integer.valueOf(reader.getElementText()));
         }
         break;
         case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME:
         {
            String value = reader.getElementText().trim();
            Validators.ADDRESS_FULL_MESSAGE_POLICY_TYPE.validate(Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME.getLocalName(),
                                                                 value);
            AddressFullMessagePolicy policy = null;
            if (value.equals(AddressFullMessagePolicy.BLOCK.toString()))
            {
               policy = AddressFullMessagePolicy.BLOCK;
            }
            else if (value.equals(AddressFullMessagePolicy.DROP.toString()))
            {
               policy = AddressFullMessagePolicy.DROP;
            }
            else if (value.equals(AddressFullMessagePolicy.PAGE.toString()))
            {
               policy = AddressFullMessagePolicy.PAGE;
            }
            addressSettings.setAddressFullMessagePolicy(policy);
         }
         break;
         case LVQ_NODE_NAME:
         {
            addressSettings.setLastValueQueue(Boolean.valueOf(reader.getElementText().trim()));
         }
         break;
         case MAX_DELIVERY_ATTEMPTS:
         {
            addressSettings.setMaxDeliveryAttempts(Integer.valueOf(reader.getElementText().trim()));
         }
         break;
         case REDISTRIBUTION_DELAY_NODE_NAME:
         {
            addressSettings.setRedistributionDelay(Long.valueOf(reader.getElementText().trim()));
         }
         break;
         case SEND_TO_DLA_ON_NO_ROUTE:
         {
            addressSettings.setSendToDLAOnNoRoute(Boolean.valueOf(reader.getElementText().trim()));
         }
         break;
         default:
            break;
         }

         reader.discardRemainder();
      } while(!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName()) && reader.getEventType() != XMLExtendedStreamReader.END_ELEMENT);
 
      return setting;
   }

}
