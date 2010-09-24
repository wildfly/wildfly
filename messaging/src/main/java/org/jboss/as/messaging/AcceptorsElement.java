package org.jboss.as.messaging;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
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
import java.util.List;
import java.util.Set;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class AcceptorsElement extends AbstractModelElement<AcceptorsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   public AcceptorsElement(final XMLExtendedStreamReader reader, Configuration config) throws XMLStreamException {
      boolean trace = log.isTraceEnabled();
      if(trace)
         log.trace("Begin " + reader.getLocation() + reader.getLocalName());
      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());
         /*
               <acceptor name="netty">
                  <factory-class>org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory</factory-class>
                  <param key="host"  value="${jboss.bind.address:localhost}"/>
                  <param key="port"  value="${hornetq.remoting.netty.port:5445}"/>
               </acceptor>
         */
         switch (element) {
         case ACCEPTOR:
            String name = reader.getAttributeValue(0);
            TransportConfiguration acceptorConfig = ElementUtils.parseTransportConfiguration(reader, name, Element.ACCEPTOR);
            config.getAcceptorConfigurations().add(acceptorConfig);
            break;
         }
      } while (reader.hasNext() && localName.equals(Element.ACCEPTOR.getLocalName()));
      if(trace)
         log.trace("End " + reader.getLocation() + reader.getLocalName());
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<AcceptorsElement> getElementClass() {
      return AcceptorsElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }
}
