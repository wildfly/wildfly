package org.jboss.as.messaging.test;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A noop implementation of XMLElementReader that simply skips over the content
 * that is passed to the NullSubsystemParser#readElement method.
 *
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemParser implements XMLElementReader<ParseResult<List<?>>> {

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<List<?>> arg1) throws XMLStreamException {
        reader.discardRemainder();
    }

}
