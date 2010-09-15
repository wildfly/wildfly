package org.jboss.as.messaging;

import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class MessagingSubsystemParser implements XMLElementReader<ParseResult<? super MessagingSubsystemElement>> {

   private static final ThreadLocal<MessagingSubsystemElement> LAST_ELEMENT = new ThreadLocal<MessagingSubsystemElement>();

   private MessagingSubsystemParser() {
   }

   private static final MessagingSubsystemParser INSTANCE = new MessagingSubsystemParser();

   /**
    * Get the instance.
    *
    * @return the instance
    */
   public static MessagingSubsystemParser getInstance() {
      return INSTANCE;
   }

   /**
    * For testing only.
    */
   public static MessagingSubsystemElement getLastSubsystemElement() {
      return LAST_ELEMENT.get();
   }
   public static void clearLastSubsystemElement() {
      LAST_ELEMENT.remove();
   }

   /**
    * {@inheritDoc}
    */
   public void readElement(final XMLExtendedStreamReader reader, final ParseResult<? super MessagingSubsystemElement> result) throws XMLStreamException {
      System.out.println("MessagingSubsystemElement.readElement");
      MessagingSubsystemElement msp = new MessagingSubsystemElement(reader);
      LAST_ELEMENT.set(msp);
      result.setResult(msp);
   }

}
