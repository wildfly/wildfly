package org.jboss.as.messaging;

import org.jboss.as.model.ParseResult;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * The messaging subsystem domain parser
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class MessagingSubsystemParser implements XMLElementReader<ParseResult<? super MessagingSubsystemElement>> {
   /** A thread local for storing the last MessagingSubsystemElement parsed for testing */
   private static final ThreadLocal<MessagingSubsystemElement> LAST_ELEMENT = new ThreadLocal<MessagingSubsystemElement>();
   private static boolean useThreadLocal = false;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

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
   public static void enableThreadLocal(boolean flag) {
      useThreadLocal = flag;
   }
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
      log.debug("MessagingSubsystemElement.readElement, event="+reader.getEventType());
      MessagingSubsystemElement msp = new MessagingSubsystemElement(reader);
      if(useThreadLocal)
         LAST_ELEMENT.set(msp);
      result.setResult(msp);
   }

}
