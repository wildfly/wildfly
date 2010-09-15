package org.jboss.as.messaging;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.messaging.hornetq.HornetQService;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: starksm Date: Sep 13, 2010 Time: 11:44:11 PM To change this template use File |
 * Settings | File Templates.
 */
public class MessagingSubsystemElement extends AbstractSubsystemElement<MessagingSubsystemElement> {

   private static final long serialVersionUID = 8225457441023207312L;

   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   /**
    * The service name "jboss.messaging".
    */
   public static final ServiceName JBOSS_MESSAGING = ServiceName.JBOSS.append("messaging");

   public static final String NAMESPACE_1_0 = "urn:jboss:domain:messaging:1.0";

   public static final String NAMESPACE = NAMESPACE_1_0;

   public static final Set<String> NAMESPACES = Collections.singleton(NAMESPACE);

   private ConfigurationElement configuration;

   /**
    * Construct a new instance.
    *
    * @param location    the declaration location of this element
    * @param elementName the name of the subsystem element
    */
   public MessagingSubsystemElement(final Location location, final QName elementName) {
      super(location, elementName);
   }

   /**
    * Construct a new instance.
    *
    * @param reader the reader from which the subsystem element should be read
    */
   public MessagingSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      super(reader);

      configuration = new ConfigurationElement(reader);
   }

   public ConfigurationElement getConfiguration() {
      return configuration;
   }

   /**
    * {@inheritDoc}
    */
   public long elementHash() {
      // TODO
      return 0L;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<MessagingSubsystemElement>> target, MessagingSubsystemElement other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   /**
    * {@inheritDoc}
    */
   protected Class<MessagingSubsystemElement> getElementClass() {
      return MessagingSubsystemElement.class;
   }

   /**
    * {@inheritDoc}
    */
   public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      streamWriter.writeStartElement("configuration");
      configuration.writeContent(streamWriter);
      streamWriter.writeEndElement();
   }

   /**
    * {@inheritDoc}
    */
   public void activate(final ServiceActivatorContext context) {
      log.info("Activating Messaging Subsystem");
      HornetQService hqservice = new HornetQService();
      Configuration hqConfig = this.configuration.getConfiguration();
      hqservice.setConfiguration(hqConfig);

      final BatchBuilder batchBuilder = context.getBatchBuilder();
      final BatchServiceBuilder<HornetQServer> serviceBuilder = batchBuilder.addService(JBOSS_MESSAGING, hqservice);
      serviceBuilder.setLocation(getLocation());
      serviceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
   }

   @Override
   public Collection<String> getReferencedSocketBindings() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

}
