package org.jboss.as.messaging.test;

import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * A noop implementation of XMLElementReader that simply skips over the content that is passed to
 * the NullSubsystemParser#readElement method.
 *
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemParser<Object> implements XMLElementReader<ParseResult<NullSubsystemElement<Object>>> {
   @Override
   public void readElement(XMLExtendedStreamReader reader, ParseResult<NullSubsystemElement<Object>> result) throws XMLStreamException {
      System.out.println(getClass().getCanonicalName()+".readElement, "+reader.getLocalName());
      NullSubsystemElement<Object> element = new NullSubsystemElement(reader);
      result.setResult(element);
   }
}
