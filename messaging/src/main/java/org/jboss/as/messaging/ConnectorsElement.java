package org.jboss.as.messaging;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class ConnectorsElement extends AbstractModelElement<ConnectorsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   public ConnectorsElement(final XMLExtendedStreamReader reader, Configuration config) throws XMLStreamException {
      log.tracef("Begin %s:%s", reader.getLocation(), reader.getLocalName());

      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());
         /*
            <connector name="netty">
               <factory-class>org.hornetq.core.remoting.impl.netty.NettyConnectorFactory</factory-class>
               <param key="host"  value="${jboss.bind.address:localhost}"/>
               <param key="port"  value="${hornetq.remoting.netty.port:5445}"/>
            </connector>
         */
         switch (element) {
         case CONNECTOR:
            String name = reader.getAttributeValue(0);
            parseConnector(reader, name, config);
            break;
         }
      } while (reader.hasNext() && localName.equals(org.jboss.as.messaging.Element.CONNECTOR.getLocalName()));
      log.tracef("End %s:%s", reader.getLocation(), reader.getLocalName());
   }


   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<ConnectorsElement> getElementClass() {
      return ConnectorsElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   private void parseConnector(final XMLExtendedStreamReader reader, String name, Configuration config)
      throws XMLStreamException {
      TransportConfiguration connectorConfig = ElementUtils.parseTransportConfiguration(reader, name, Element.CONNECTOR);
      if (connectorConfig.getName() == null) {
         log.warn("Cannot deploy a connector with no name specified.");
      }

      if (config.getConnectorConfigurations().containsKey(connectorConfig.getName())) {
         log.warn("There is already a connector with name " + connectorConfig.getName() +
            " deployed. This one will not be deployed.");
      }

      config.getConnectorConfigurations().put(connectorConfig.getName(), connectorConfig);
   }


}
