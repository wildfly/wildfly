package org.jboss.as.messaging;

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

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class AcceptorsElement extends AbstractModelElement<AcceptorsElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   Configuration config = new ConfigurationImpl();


   public AcceptorsElement(final Location location) {
      super(location);
   }

   public AcceptorsElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      super(reader);
      System.out.println("Begin "+reader.getLocation()+reader.getLocalName());
      reader.discardRemainder();
   }

   public List<ConnectorServiceConfiguration> getConnectorServiceConfigurations() {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<AcceptorsElement>> target, AcceptorsElement other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<AcceptorsElement> getElementClass() {
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
}
