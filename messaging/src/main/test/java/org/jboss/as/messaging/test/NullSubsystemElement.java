package org.jboss.as.messaging.test;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemElement<T> extends AbstractSubsystemElement<NullSubsystemElement<T>> {

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
   protected void appendDifference(Collection<AbstractModelUpdate<NullSubsystemElement<T>>> target, NullSubsystemElement<T> other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<NullSubsystemElement<T>> getElementClass() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }
}
