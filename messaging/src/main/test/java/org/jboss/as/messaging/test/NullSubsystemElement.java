package org.jboss.as.messaging.test;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemElement<T> extends AbstractSubsystemElement<NullSubsystemElement<Object>> {

   public NullSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      super(reader);
      reader.discardRemainder();
   }

   @Override
   public void activate(ServiceActivatorContext context) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<NullSubsystemElement<Object>> getElementClass() {
      Class<NullSubsystemElement<Object>> c = (Class<NullSubsystemElement<Object>>) getClass();
      return c;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }
}
