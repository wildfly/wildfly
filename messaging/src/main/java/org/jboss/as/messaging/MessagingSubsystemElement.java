package org.jboss.as.messaging;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.Set;

/**
 * The subsystem element for the messaging configuration and activation.
 *
 * @author scott.stark@jboss.org
 * @version $Id$
 */
public class MessagingSubsystemElement extends AbstractSubsystemElement<MessagingSubsystemElement> {

   private static final long serialVersionUID = 8225457441023207312L;

   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   /**
    * The service name "jboss.messaging".
    */
   public static final ServiceName JBOSS_MESSAGING = ServiceName.JBOSS.append("messaging");
   /** The subsystem configration namespace in the domain */
   public static final String NAMESPACE_1_0 = "urn:jboss:domain:messaging:1.0";

   public static final String NAMESPACE = NAMESPACE_1_0;

   public static final Set<String> NAMESPACES = Collections.singleton(NAMESPACE);
   /** The */
   private ConfigurationElement configuration;

   /**
    * Construct a new instance.
    *
    * @param reader the reader from which the subsystem element should be read
    * @throws XMLStreamException
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
   @Override
   private long elementHash() {
      // TODO
      return 0L;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected Class<MessagingSubsystemElement> getElementClass() {
      return MessagingSubsystemElement.class;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      configuration.writeContent(streamWriter);
      streamWriter.writeEndElement();
   }
}
